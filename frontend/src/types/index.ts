// 백엔드 DTO/enum과 1:1 대응. backend/src/main/java/com/alrdream/domain/**/api/dto 기준.

export type MemberRole = "USER" | "ADMIN";
export type MemberPlan = "FREE" | "PRO";
export type AuthProvider = "LOCAL" | "GOOGLE" | "APPLE";

export interface Member {
  id: string;
  email: string;
  role: MemberRole;
  plan: MemberPlan;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export type WorkspaceStatus = "ACTIVE";

export interface Workspace {
  id: string;
  name: string;
  status: WorkspaceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PageInfo {
  size: number;
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface Page<T> {
  content: T[];
  page: PageInfo;
}

export type SurveyKey = "PLANNING_HAS_IDEA" | "PLANNING_EXPLORING" | "DESIGN";
export type QuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "SHORT_TEXT" | "LONG_TEXT" | "SCALE";

export interface QuestionOption {
  key: string;
  label: string;
}

export interface Question {
  id: string;
  promptKey: string;
  type: QuestionType;
  question: string;
  required: boolean;
  options: QuestionOption[];
  allowUnknown: boolean;
}

export interface SurveyDefinition {
  id: string;
  surveyKey: SurveyKey;
  version: number;
  title: string;
  questions: Question[];
  createdAt: string;
}

export interface SurveyAnswer {
  questionId: string;
  type: QuestionType;
  values: string[];
  isUnknown: boolean;
}

export interface SurveyResponseSummary {
  id: string;
  surveyDefinitionId: string;
  submittedAt: string;
}

export interface SurveyResponseDetail extends SurveyResponseSummary {
  answers: SurveyAnswer[];
}

export type VersionStatus = "GENERATING" | "COMPLETED" | "FAILED";

export interface PlanningVersionSummary {
  id: string;
  versionNo: number;
  status: VersionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PlanningVersionDetail extends PlanningVersionSummary {
  workspaceId: string;
  surveyResponseId: string;
  content: PlanningContent | null;
}

export interface AnalysisVersionSummary {
  id: string;
  versionNo: number;
  status: VersionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AnalysisVersionDetail extends AnalysisVersionSummary {
  planningVersionId: string;
  content: AnalysisContent | null;
}

export interface DesignVersionSummary {
  id: string;
  versionNo: number;
  status: VersionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DesignVersionDetail extends DesignVersionSummary {
  analysisVersionId: string;
  surveyResponseId: string;
  content: DesignContent | null;
}

export type AiTargetType = "PLANNING" | "ANALYSIS" | "DESIGN";
export type JobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface AiGenerationJob {
  id: string;
  targetType: AiTargetType;
  targetId: string;
  status: JobStatus;
  errorMessage: string | null;
  createdAt: string;
}

export interface DocumentResponse {
  downloadUrl: string;
  generatedAt: string;
}

// ---- [01] 12-4 기획안 content 구조 (prompt_templates.schema_json 기준) ----

export interface PlanningContent {
  idea_summary: {
    one_line_pitch: string;
    problem_to_solve: string;
    target_customer: string;
    core_value: string;
  };
  problem_definition: {
    what: string;
    why_important: string;
    frequency: string;
  };
  target_customer_analysis: {
    main_users: string;
    characteristics: string;
    usage_scenario: string;
  };
  solution_proposal: {
    service_overview: string;
    core_features: string[];
    user_flow: string;
  };
  service_structure: {
    platform_type: string;
    feature_blocks: string[];
  };
  competitive_analysis: {
    existing_alternatives: string;
    competitors: string;
    differentiation: string;
  };
  revenue_model: {
    how_to_earn: string;
    feasibility: string;
  };
  mvp_strategy: {
    minimum_features: string[];
    launch_scope: string;
  };
  execution_roadmap: { stage: string; actions: string }[];
  risks_and_improvements: {
    weaknesses: string;
    risks: string;
    needs_further_research: string;
  };
}

export interface AnalysisContent {
  legality: {
    is_legal: string;
    considerations: string;
    required_licenses_or_permits: string;
  };
  resource_availability: {
    material_resources: string;
    human_resources: string;
    overall_feasibility: string;
  };
  competitive_landscape: {
    existing_competitors: string;
    market_saturation: string;
    competitive_advantage: string;
  };
  core_feature_candidates: { key: string; label: string }[];
  overall_assessment: string;
}

export interface DesignContent {
  feature_specification: { feature: string; description: string; priority: string }[];
  mvp_definition: {
    scope: string;
    excluded_for_later: string;
  };
  technical_architecture: {
    recommended_stack: string;
    architecture_overview: string;
    constraints_reflected: string;
  };
  platform_plan: {
    primary_platform: string;
    rationale: string;
  };
  data_and_privacy: {
    sensitive_data_handled: string;
    protection_measures: string;
  };
  system_structure: string[];
  development_plan: { phase: string; tasks: string }[];
}
