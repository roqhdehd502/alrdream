package com.alrdream.domain.design.application;

/**
 * [01] 10번 — "정해진 형태에 맞게 설계 문서". 문서에 콘텐츠 구조가 명시돼 있지 않아, [02] §5-3 DESIGN 설문
 * 문항(핵심 기능 우선순위/MVP 범위/기술 제약/플랫폼/데이터 민감도)이 각각 대응되는 구체적인 설계 산출물로
 * 직접 설계했다.
 */
final class DesignGenerationSpec {

	static final String TOOL_NAME = "emit_design_document";
	static final String TOOL_DESCRIPTION = "기획/분석 결과와 설계 설문 응답을 바탕으로 구조화된 설계 문서를 반환한다.";

	static final String SYSTEM_PROMPT = """
			당신은 사업 기획·분석 내용을 실제로 개발 가능한 서비스 설계 문서로 구체화해주는 AI다.

			입력은 세 부분으로 주어진다.
			1. [기획 단계 설문 응답]: 최초 사업 아이디어에 대한 사용자의 원본 답변 (promptKey: 값)
			2. [분석 결과]: 합법성/가용 자원/경쟁 구도/핵심 기능 후보를 담은 이전 단계 분석 결과 (JSON)
			3. [설계 설문 응답]: 사용자가 이번 설계 단계에서 고른 답변 — 우선순위 높은 핵심 기능(분석의 기능
			   후보 중 선택), MVP 범위, 기술/일정/예산 제약, 우선 출시 플랫폼, 민감 데이터 여부

			다음을 반영해 설계 문서를 작성하라.
			- 사용자가 설계 설문에서 고른 핵심 기능들을 중심으로 각 기능의 구체적인 명세를 작성한다.
			- MVP 범위는 사용자가 밝힌 범위를 존중하되, 실행 가능한 수준으로 구체화한다.
			- 기술/일정/예산 제약을 반드시 반영해 현실적인 기술 스택과 아키텍처를 제안한다.
			- 우선 출시 플랫폼에 맞는 구조를 제안한다.
			- 민감 데이터가 있다고 답했다면 구체적인 보호 방안을 제시한다("없음"이면 "해당 없음"으로 짧게 적는다).
			- 모든 내용은 한국어로 작성하고, 막연한 이야기가 아니라 실제 개발 착수가 가능한 수준으로 구체적이어야 한다.

			주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
			""";

	static final String SCHEMA_JSON = """
			{
			  "type": "object",
			  "properties": {
			    "feature_specification": {
			      "type": "array",
			      "description": "설계 설문에서 우선순위로 선택된 핵심 기능별 상세 명세",
			      "items": {
			        "type": "object",
			        "properties": {
			          "feature": {"type": "string", "description": "기능 이름"},
			          "description": {"type": "string", "description": "상세 설명 (동작 방식, 화면 구성 등)"},
			          "priority": {"type": "string", "description": "우선순위 (예: 필수/권장/선택)"}
			        },
			        "required": ["feature", "description", "priority"]
			      }
			    },
			    "mvp_definition": {
			      "type": "object",
			      "properties": {
			        "scope": {"type": "string", "description": "MVP에 포함되는 범위"},
			        "excluded_for_later": {"type": "string", "description": "MVP에서 제외하고 이후 반영할 항목"}
			      },
			      "required": ["scope", "excluded_for_later"]
			    },
			    "technical_architecture": {
			      "type": "object",
			      "properties": {
			        "recommended_stack": {"type": "string", "description": "권장 기술 스택"},
			        "architecture_overview": {"type": "string", "description": "아키텍처 개요"},
			        "constraints_reflected": {"type": "string", "description": "사용자가 밝힌 기술/일정/예산 제약이 어떻게 반영됐는지"}
			      },
			      "required": ["recommended_stack", "architecture_overview", "constraints_reflected"]
			    },
			    "platform_plan": {
			      "type": "object",
			      "properties": {
			        "primary_platform": {"type": "string", "description": "우선 출시 플랫폼"},
			        "rationale": {"type": "string", "description": "선정 이유"}
			      },
			      "required": ["primary_platform", "rationale"]
			    },
			    "data_and_privacy": {
			      "type": "object",
			      "properties": {
			        "sensitive_data_handled": {"type": "string", "description": "다루게 될 민감 데이터 (없으면 \\"해당 없음\\")"},
			        "protection_measures": {"type": "string", "description": "보호 방안 (없으면 \\"해당 없음\\")"}
			      },
			      "required": ["sensitive_data_handled", "protection_measures"]
			    },
			    "system_structure": {
			      "type": "array",
			      "description": "화면/기능 블록 구조",
			      "items": {"type": "string"}
			    },
			    "development_plan": {
			      "type": "array",
			      "description": "개발 단계별 계획",
			      "items": {
			        "type": "object",
			        "properties": {
			          "phase": {"type": "string"},
			          "tasks": {"type": "string"}
			        },
			        "required": ["phase", "tasks"]
			      }
			    }
			  },
			  "required": [
			    "feature_specification", "mvp_definition", "technical_architecture", "platform_plan",
			    "data_and_privacy", "system_structure", "development_plan"
			  ]
			}
			""";

	private DesignGenerationSpec() {
	}
}
