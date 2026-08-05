package com.alrdream.domain.analysis.application;

/**
 * [01] 7번 — "합법여부, 가용 리소스(물적, 인적) 및 경쟁 서비스 여부"를 포함한 기본 분석. 문서에 콘텐츠 구조가
 * 명시돼 있지 않아, 7번이 명시한 세 항목 + [02] §5-3에서 필요로 하는 {@code core_feature_candidates}(설계
 * 단계 동적 옵션의 원천) + 종합 의견으로 직접 설계했다.
 */
final class AnalysisGenerationSpec {

	static final String TOOL_NAME = "emit_analysis_document";
	static final String TOOL_DESCRIPTION = "기획안 본문을 바탕으로 구조화된 사업 분석을 반환한다.";

	static final String SYSTEM_PROMPT = """
			당신은 사업 기획안을 검토해 실행 전 점검이 필요한 사항을 분석해주는 AI다.

			입력으로 [01] 12-4 구조를 따르는 기획안 전체(JSON)가 주어진다. 이 내용을 바탕으로 아래 항목을
			분석하라.
			- 합법 여부: 이 사업을 한국에서 운영하는 데 법적 문제가 없는지, 필요한 인허가/신고 사항이 있는지.
			- 가용 리소스: 이 사업을 시작하는 데 필요한 물적 자원(자본, 장비, 공간 등)과 인적 자원(필요 인력
			  구성, 전문성)이 무엇이고 확보 가능성은 어떤지.
			- 경쟁 서비스: 기획안에 언급된 경쟁/대체 서비스를 포함해 시장에 이미 존재하는 경쟁 구도와 이 사업이
			  가질 수 있는 경쟁 우위.
			- 핵심 기능 후보: 기획안의 솔루션 제안/서비스 구조를 바탕으로, 다음 설계 단계에서 우선순위를 정할
			  구체적인 기능 후보 3~8개를 뽑아라. 각 후보는 영문 대문자 스네이크케이스 key(예: REALTIME_CHAT)와
			  한국어 label로 표현한다.
			- 종합 의견: 위 내용을 종합해 이 사업을 실행할 만한지에 대한 의견.

			모든 내용은 한국어로 작성하고(핵심 기능 후보의 key는 예외), 막연한 이야기가 아니라 기획안 내용에
			근거한 구체적인 분석이어야 한다. 주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
			""";

	static final String SCHEMA_JSON = """
			{
			  "type": "object",
			  "properties": {
			    "legality": {
			      "type": "object",
			      "properties": {
			        "is_legal": {"type": "string", "description": "합법 여부 및 근거"},
			        "considerations": {"type": "string", "description": "법적으로 유의해야 할 사항"},
			        "required_licenses_or_permits": {"type": "string", "description": "필요한 인허가/신고 사항 (없으면 \\"없음\\")"}
			      },
			      "required": ["is_legal", "considerations", "required_licenses_or_permits"]
			    },
			    "resource_availability": {
			      "type": "object",
			      "properties": {
			        "material_resources": {"type": "string", "description": "필요한 물적 자원(자본, 장비, 공간 등)과 확보 가능성"},
			        "human_resources": {"type": "string", "description": "필요한 인적 자원(인력 구성, 전문성)과 확보 가능성"},
			        "overall_feasibility": {"type": "string", "description": "종합적인 자원 확보 가능성 평가"}
			      },
			      "required": ["material_resources", "human_resources", "overall_feasibility"]
			    },
			    "competitive_landscape": {
			      "type": "object",
			      "properties": {
			        "existing_competitors": {"type": "string", "description": "기존 경쟁/대체 서비스"},
			        "market_saturation": {"type": "string", "description": "시장 포화도"},
			        "competitive_advantage": {"type": "string", "description": "이 사업이 가질 수 있는 경쟁 우위"}
			      },
			      "required": ["existing_competitors", "market_saturation", "competitive_advantage"]
			    },
			    "core_feature_candidates": {
			      "type": "array",
			      "description": "설계 단계에서 우선순위를 정할 핵심 기능 후보 (3~8개)",
			      "items": {
			        "type": "object",
			        "properties": {
			          "key": {"type": "string", "description": "영문 대문자 스네이크케이스 식별자, 예: REALTIME_CHAT"},
			          "label": {"type": "string", "description": "한국어 기능 이름"}
			        },
			        "required": ["key", "label"]
			      }
			    },
			    "overall_assessment": {"type": "string", "description": "종합 의견"}
			  },
			  "required": [
			    "legality", "resource_availability", "competitive_landscape",
			    "core_feature_candidates", "overall_assessment"
			  ]
			}
			""";

	private AnalysisGenerationSpec() {
	}
}
