import { useEffect, useState } from "react";
import { surveyDefinitionsApi } from "../api/surveyDefinitions";
import { ApiError } from "../api/client";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { Question, QuestionType, SurveyDefinitionResponse, SurveyKey } from "../types";

const SURVEY_KEYS: { key: SurveyKey; label: string }[] = [
  { key: "PLANNING_HAS_IDEA", label: "기획 - 아이템 있음" },
  { key: "PLANNING_EXPLORING", label: "기획 - 고민 중" },
  { key: "DESIGN", label: "설계" },
];

const QUESTION_TYPES: QuestionType[] = ["SINGLE_CHOICE", "MULTI_CHOICE", "SHORT_TEXT", "LONG_TEXT", "SCALE"];

function emptyQuestion(): Question {
  return {
    id: "",
    promptKey: "",
    type: "SHORT_TEXT",
    question: "",
    required: true,
    options: [],
    allowUnknown: false,
  };
}

function QuestionEditor({
  question,
  onChange,
  onRemove,
}: {
  question: Question;
  onChange: (q: Question) => void;
  onRemove: () => void;
}) {
  const needsOptions = question.type === "SINGLE_CHOICE" || question.type === "MULTI_CHOICE";

  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div className="form-row">
        <div className="form-field">
          <label>문항 ID</label>
          <input
            type="text"
            value={question.id}
            onChange={(e) => onChange({ ...question, id: e.target.value })}
            placeholder="Q1"
          />
        </div>
        <div className="form-field">
          <label>promptKey</label>
          <input
            type="text"
            value={question.promptKey}
            onChange={(e) => onChange({ ...question, promptKey: e.target.value })}
            placeholder="idea_summary"
          />
        </div>
        <div className="form-field">
          <label>타입</label>
          <select
            value={question.type}
            onChange={(e) => onChange({ ...question, type: e.target.value as QuestionType })}
          >
            {QUESTION_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="form-field">
        <label>질문 텍스트</label>
        <input
          type="text"
          value={question.question}
          onChange={(e) => onChange({ ...question, question: e.target.value })}
        />
      </div>

      <div className="form-row">
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={question.required}
            onChange={(e) => onChange({ ...question, required: e.target.checked })}
          />
          필수 응답
        </label>
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={question.allowUnknown}
            onChange={(e) => onChange({ ...question, allowUnknown: e.target.checked })}
          />
          "잘 모르겠어요" 허용
        </label>
      </div>

      {needsOptions && (
        <div className="form-field">
          <label>보기 (key / label)</label>
          {question.options.map((opt, idx) => (
            <div key={idx} className="form-row" style={{ marginBottom: 6 }}>
              <input
                type="text"
                value={opt.key}
                placeholder="key"
                onChange={(e) => {
                  const options = [...question.options];
                  options[idx] = { ...options[idx], key: e.target.value };
                  onChange({ ...question, options });
                }}
              />
              <input
                type="text"
                value={opt.label}
                placeholder="label"
                onChange={(e) => {
                  const options = [...question.options];
                  options[idx] = { ...options[idx], label: e.target.value };
                  onChange({ ...question, options });
                }}
              />
              <button
                type="button"
                className="btn"
                onClick={() => onChange({ ...question, options: question.options.filter((_, i) => i !== idx) })}
              >
                삭제
              </button>
            </div>
          ))}
          <button
            type="button"
            className="btn"
            onClick={() => onChange({ ...question, options: [...question.options, { key: "", label: "" }] })}
          >
            + 보기 추가
          </button>
        </div>
      )}

      <button type="button" className="btn btn-danger" onClick={onRemove} style={{ marginTop: 4 }}>
        문항 삭제
      </button>
    </div>
  );
}

export function SurveyDefinitionsPage() {
  const [selectedKey, setSelectedKey] = useState<SurveyKey>("PLANNING_HAS_IDEA");
  const [versions, setVersions] = useState<SurveyDefinitionResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [preview, setPreview] = useState<SurveyDefinitionResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [questions, setQuestions] = useState<Question[]>([]);
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState<string | null>(null);

  const loadVersions = () => {
    setVersions(null);
    surveyDefinitionsApi
      .list(selectedKey)
      .then(setVersions)
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(() => {
    loadVersions();
    setPreview(null);
    setEditing(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedKey]);

  const startEditing = () => {
    const latest = versions?.[0];
    setTitle(latest?.title ?? "");
    setQuestions(latest ? latest.questions.map((q) => ({ ...q, options: q.options.map((o) => ({ ...o })) })) : [emptyQuestion()]);
    setPublishError(null);
    setEditing(true);
    setPreview(null);
  };

  const publish = async () => {
    setPublishing(true);
    setPublishError(null);
    try {
      await surveyDefinitionsApi.publish({ surveyKey: selectedKey, title, questions });
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
        title="설문 정의 관리"
        description="발행된 버전은 수정할 수 없습니다 — 문항을 바꾸려면 새 버전을 발행하세요."
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
        {SURVEY_KEYS.map((sk) => (
          <button
            key={sk.key}
            type="button"
            className={`tab-button${selectedKey === sk.key ? " active" : ""}`}
            onClick={() => setSelectedKey(sk.key)}
          >
            {sk.label}
          </button>
        ))}
      </div>

      {editing ? (
        <div className="card">
          <div className="form-field">
            <label>설문 제목</label>
            <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          {questions.map((q, idx) => (
            <QuestionEditor
              key={idx}
              question={q}
              onChange={(updated) => setQuestions(questions.map((qq, i) => (i === idx ? updated : qq)))}
              onRemove={() => setQuestions(questions.filter((_, i) => i !== idx))}
            />
          ))}

          <button type="button" className="btn" onClick={() => setQuestions([...questions, emptyQuestion()])}>
            + 문항 추가
          </button>

          <ErrorAlert message={publishError} />

          <div className="toolbar" style={{ marginTop: 16 }}>
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
              v{preview.version} — {preview.title}
            </h3>
            <button type="button" className="btn" onClick={() => setPreview(null)}>
              닫기
            </button>
          </div>
          {preview.questions.map((q) => (
            <div key={q.id} style={{ padding: "10px 0", borderBottom: "1px solid var(--color-border)" }}>
              <div style={{ fontWeight: 600 }}>
                {q.id}. {q.question} {q.required && <span style={{ color: "var(--color-danger)" }}>*</span>}
              </div>
              <div style={{ fontSize: 13, color: "var(--color-text-muted)" }}>
                {q.type} · promptKey={q.promptKey} {q.allowUnknown && "· 모름 허용"}
              </div>
              {q.options.length > 0 && (
                <ul style={{ margin: "6px 0 0", paddingLeft: 20 }}>
                  {q.options.map((o) => (
                    <li key={o.key}>
                      {o.label} ({o.key})
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      ) : versions === null ? (
        <Loading />
      ) : versions.length === 0 ? (
        <EmptyState label="아직 발행된 설문이 없습니다." />
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>버전</th>
                <th>제목</th>
                <th>문항 수</th>
                <th>발행 시각</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {versions.map((v, idx) => (
                <tr key={v.id} className="clickable" onClick={() => setPreview(v)}>
                  <td>
                    v{v.version} {idx === 0 && <span className="badge badge-primary">최신</span>}
                  </td>
                  <td>{v.title}</td>
                  <td>{v.questions.length}</td>
                  <td>{new Date(v.createdAt).toLocaleString("ko-KR")}</td>
                  <td>미리보기 →</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
