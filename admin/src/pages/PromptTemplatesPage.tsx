import { useEffect, useState } from "react";
import { promptTemplatesApi } from "../api/promptTemplates";
import { ApiError } from "../api/client";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { AiTargetType, PromptTemplateResponse } from "../types";

const PROMPT_TYPES: { key: AiTargetType; label: string }[] = [
  { key: "PLANNING", label: "기획" },
  { key: "ANALYSIS", label: "분석" },
  { key: "DESIGN", label: "설계" },
];

function prettyJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

export function PromptTemplatesPage() {
  const [selectedType, setSelectedType] = useState<AiTargetType>("PLANNING");
  const [versions, setVersions] = useState<PromptTemplateResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [preview, setPreview] = useState<PromptTemplateResponse | null>(null);
  const [editing, setEditing] = useState(false);

  const [toolName, setToolName] = useState("");
  const [toolDescription, setToolDescription] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("");
  const [schemaJson, setSchemaJson] = useState("");
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState<string | null>(null);

  const loadVersions = () => {
    setVersions(null);
    promptTemplatesApi
      .list(selectedType)
      .then(setVersions)
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(() => {
    loadVersions();
    setPreview(null);
    setEditing(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedType]);

  const startEditing = () => {
    const latest = versions?.[0];
    setToolName(latest?.toolName ?? "");
    setToolDescription(latest?.toolDescription ?? "");
    setSystemPrompt(latest?.systemPrompt ?? "");
    setSchemaJson(latest ? prettyJson(latest.schemaJson) : "{\n  \"type\": \"object\"\n}");
    setPublishError(null);
    setEditing(true);
    setPreview(null);
  };

  const publish = async () => {
    try {
      JSON.parse(schemaJson);
    } catch {
      setPublishError("JSON Schema가 올바른 JSON 형식이 아닙니다.");
      return;
    }
    setPublishing(true);
    setPublishError(null);
    try {
      await promptTemplatesApi.publish({
        promptType: selectedType,
        toolName,
        toolDescription,
        systemPrompt,
        schemaJson,
      });
      setEditing(false);
      loadVersions();
    } catch (e) {
      setPublishError(e instanceof ApiError ? e.message : "발행에 실패했습니다.");
    } finally {
      setPublishing(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="AI 프롬프트 템플릿 관리"
        description="기획/분석/설계 생성에 실제로 쓰이는 시스템 프롬프트와 Claude Tool Use 스키마입니다. 새 버전을 발행하면 즉시 다음 생성부터 적용됩니다(활성 버전 = 최신 버전). 발행된 버전은 수정할 수 없습니다."
        action={
          !editing && (
            <button type="button" className="btn btn-primary" onClick={startEditing}>
              + 새 버전 발행
            </button>
          )
        }
      />

      <ErrorAlert message={error} />

      <div className="tabs">
        {PROMPT_TYPES.map((pt) => (
          <button
            key={pt.key}
            type="button"
            className={`tab-button${selectedType === pt.key ? " active" : ""}`}
            onClick={() => setSelectedType(pt.key)}
          >
            {pt.label}
          </button>
        ))}
      </div>

      {editing ? (
        <div className="card">
          <div className="form-row">
            <div className="form-field">
              <label>Tool 이름</label>
              <input type="text" value={toolName} onChange={(e) => setToolName(e.target.value)} />
            </div>
            <div className="form-field">
              <label>Tool 설명</label>
              <input type="text" value={toolDescription} onChange={(e) => setToolDescription(e.target.value)} />
            </div>
          </div>
          <div className="form-field">
            <label>시스템 프롬프트</label>
            <textarea rows={14} value={systemPrompt} onChange={(e) => setSystemPrompt(e.target.value)} />
          </div>
          <div className="form-field">
            <label>JSON Schema (Claude Tool Use input_schema)</label>
            <textarea rows={18} value={schemaJson} onChange={(e) => setSchemaJson(e.target.value)} />
          </div>

          <ErrorAlert message={publishError} />

          <div className="toolbar">
            <button type="button" className="btn btn-primary" onClick={publish} disabled={publishing}>
              {publishing ? "발행 중..." : "발행"}
            </button>
            <button type="button" className="btn" onClick={() => setEditing(false)}>
              취소
            </button>
          </div>
        </div>
      ) : preview ? (
        <div className="card">
          <div className="toolbar" style={{ justifyContent: "space-between" }}>
            <h3 style={{ margin: 0 }}>
              v{preview.version} — {preview.toolName}
            </h3>
            <button type="button" className="btn" onClick={() => setPreview(null)}>
              닫기
            </button>
          </div>
          <p style={{ color: "var(--color-text-muted)" }}>{preview.toolDescription}</p>
          <div className="form-field">
            <label>시스템 프롬프트</label>
            <textarea rows={12} readOnly value={preview.systemPrompt} />
          </div>
          <div className="form-field">
            <label>JSON Schema</label>
            <textarea rows={16} readOnly value={prettyJson(preview.schemaJson)} />
          </div>
        </div>
      ) : versions === null ? (
        <Loading />
      ) : versions.length === 0 ? (
        <EmptyState label="발행된 템플릿이 없습니다." />
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>버전</th>
                <th>Tool 이름</th>
                <th>발행 시각</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {versions.map((v, idx) => (
                <tr key={v.id} className="clickable" onClick={() => setPreview(v)}>
                  <td>
                    v{v.version} {idx === 0 && <span className="badge badge-primary">활성</span>}
                  </td>
                  <td>{v.toolName}</td>
                  <td>{new Date(v.createdAt).toLocaleString("ko-KR")}</td>
                  <td>상세 보기 →</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
