package com.alrdream.domain.planning.application;

/** [01] 12-4 PDF 구조를 Claude Tool Use의 시스템 프롬프트/JSON Schema로 옮긴 것. */
final class PlanningGenerationSpec {

	static final String TOOL_NAME = "emit_planning_document";
	static final String TOOL_DESCRIPTION = "사용자의 설문 응답을 바탕으로 구조화된 사업 기획안을 반환한다.";

	static final String SYSTEM_PROMPT = """
			당신은 사용자의 사업 아이디어를 실행 가능한 사업 기획서로 변환해주는 AI다.

			입력으로 "promptKey: 답변" 형태의 설문 응답 목록이 주어진다. 답변 값이
			"(사용자가 확신하지 못함 — AI가 조사/제안 필요)"로 표시된 항목은 사용자가 확신하지 못하는 부분이니,
			일반적인 시장 정보와 상식을 바탕으로 조사/제안하고 이 사실을 "리스크 & 보완 포인트" 섹션에 반영하라.

			다음 원칙을 반드시 지켜라.
			- 모든 내용은 한국어로 작성한다.
			- "아이디어 정리해드립니다" 수준의 막연한 요약이 아니라, "사업을 시작할 수 있는 수준"의 구체적이고
			  실행 가능한 내용으로 작성한다.
			- 타겟 고객은 "직장인"처럼 막연하게 쓰지 말고 "야근이 많은 20~30대 직장인"처럼 구체적으로 쓴다.
			- 수익 모델은 현실성 있는 구조를 AI가 직접 제안해야 한다.
			- MVP 전략은 반드시 포함한다 — 없으면 쓸모없는 문서가 된다.
			- 실행 로드맵은 기획/개발/출시 등 단계별 액션을 포함한다.

			주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
			""";

	// [01] 12-4 구조 그대로 10개 섹션을 강제한다.
	static final String SCHEMA_JSON = """
			{
			  "type": "object",
			  "properties": {
			    "idea_summary": {
			      "type": "object",
			      "properties": {
			        "one_line_pitch": {"type": "string", "description": "한 줄 아이디어"},
			        "problem_to_solve": {"type": "string", "description": "해결하려는 문제"},
			        "target_customer": {"type": "string", "description": "타겟 고객"},
			        "core_value": {"type": "string", "description": "핵심 가치"}
			      },
			      "required": ["one_line_pitch", "problem_to_solve", "target_customer", "core_value"]
			    },
			    "problem_definition": {
			      "type": "object",
			      "properties": {
			        "what": {"type": "string", "description": "어떤 문제가 있는가"},
			        "why_important": {"type": "string", "description": "왜 중요한가"},
			        "frequency": {"type": "string", "description": "얼마나 자주 발생하는가"}
			      },
			      "required": ["what", "why_important", "frequency"]
			    },
			    "target_customer_analysis": {
			      "type": "object",
			      "properties": {
			        "main_users": {"type": "string", "description": "주요 사용자"},
			        "characteristics": {"type": "string", "description": "사용자 특징"},
			        "usage_scenario": {"type": "string", "description": "사용 시나리오"}
			      },
			      "required": ["main_users", "characteristics", "usage_scenario"]
			    },
			    "solution_proposal": {
			      "type": "object",
			      "properties": {
			        "service_overview": {"type": "string", "description": "서비스 개요"},
			        "core_features": {"type": "array", "items": {"type": "string"}, "description": "핵심 기능 3~5개"},
			        "user_flow": {"type": "string", "description": "사용자 흐름 (간단)"}
			      },
			      "required": ["service_overview", "core_features", "user_flow"]
			    },
			    "service_structure": {
			      "type": "object",
			      "properties": {
			        "platform_type": {"type": "string", "description": "웹 / 앱 / SaaS 형태"},
			        "feature_blocks": {"type": "array", "items": {"type": "string"}, "description": "주요 기능 블록 (예: 로그인, 콘텐츠 생성, 저장, 공유)"}
			      },
			      "required": ["platform_type", "feature_blocks"]
			    },
			    "competitive_analysis": {
			      "type": "object",
			      "properties": {
			        "existing_alternatives": {"type": "string", "description": "기존 해결 방법"},
			        "competitors": {"type": "string", "description": "경쟁 서비스"},
			        "differentiation": {"type": "string", "description": "차별점"}
			      },
			      "required": ["existing_alternatives", "competitors", "differentiation"]
			    },
			    "revenue_model": {
			      "type": "object",
			      "properties": {
			        "how_to_earn": {"type": "string", "description": "어떻게 돈 버는지"},
			        "feasibility": {"type": "string", "description": "현실성 있는 구조에 대한 설명"}
			      },
			      "required": ["how_to_earn", "feasibility"]
			    },
			    "mvp_strategy": {
			      "type": "object",
			      "properties": {
			        "minimum_features": {"type": "array", "items": {"type": "string"}, "description": "최소 기능 정의"},
			        "launch_scope": {"type": "string", "description": "1차 출시 범위"}
			      },
			      "required": ["minimum_features", "launch_scope"]
			    },
			    "execution_roadmap": {
			      "type": "array",
			      "description": "단계별 액션 (예: 1단계 기획, 2단계 개발, 3단계 출시)",
			      "items": {
			        "type": "object",
			        "properties": {
			          "stage": {"type": "string"},
			          "actions": {"type": "string"}
			        },
			        "required": ["stage", "actions"]
			      }
			    },
			    "risks_and_improvements": {
			      "type": "object",
			      "properties": {
			        "weaknesses": {"type": "string", "description": "부족한 점"},
			        "risks": {"type": "string", "description": "위험 요소"},
			        "needs_further_research": {"type": "string", "description": "추가 조사 필요 영역 — \\"모름\\" 응답 활용"}
			      },
			      "required": ["weaknesses", "risks", "needs_further_research"]
			    }
			  },
			  "required": [
			    "idea_summary", "problem_definition", "target_customer_analysis", "solution_proposal",
			    "service_structure", "competitive_analysis", "revenue_model", "mvp_strategy",
			    "execution_roadmap", "risks_and_improvements"
			  ]
			}
			""";

	private PlanningGenerationSpec() {
	}
}
