<!-- 01_planning_and_analysis.md, 02_ai_survey.md 그리고 03_design.md를 참고 하여, 마일스톤을 작성합니다. -->
<!-- 마일스톤 작성 양식은 TODO 형태를 포맷으로 하여 작성합니다. -->

<!-- 예시 -->

<!--
# Phase 00: GitHub Actions CI/CD 자동 배포 셋업

> 2026-07-10 수행. main 브랜치 push 시 OCI VM에 자동으로 배포되도록 GitHub Actions 워크플로우 구성.

## 변경 내역

- [x] `.github/workflows/deploy.yml` 신규 작성 — `on: push: branches: [main]` 트리거, `appleboy/ssh-action@v1.0.3`으로 OCI VM SSH 접속 후 `git reset --hard origin/main` + `docker compose -f docker-compose.prod.yml up -d --build --remove-orphans` 실행
- [x] `README.md` 업데이트 — 기술 스택 표 CI/CD 행 업데이트, "자동 배포 (GitHub Actions)" 섹션 추가 (Secrets 설정 가이드 포함), 수동 배포 명령도 `git reset --hard`로 일원화

## GitHub Secrets 설정 (GitHub 레포 → Settings → Secrets and variables → Actions)

| Secret                | 값 예시                             | 설명                 |
| --------------------- | ----------------------------------- | -------------------- |
| `OCI_SSH_HOST`        | `144.xxx.xxx.xxx`                   | OCI VM 공개 IP       |
| `OCI_SSH_USER`        | `ubuntu`                            | SSH 접속 사용자명    |
| `OCI_SSH_PRIVATE_KEY` | `-----BEGIN OPENSSH PRIVATE KEY...` | SSH 개인키 전체 내용 |
| `OCI_DEPLOY_PATH`     | `/opt/tone-knob`                    | VM 레포 클론 경로    |

## 배포 흐름

```
main push
  → GitHub Actions 트리거
    → appleboy/ssh-action (SSH 접속)
      → git fetch origin && git reset --hard origin/main
      → docker compose -f docker-compose.prod.yml up -d --build --remove-orphans
```

## 미완료 선제 조건 (사용자 측)

- OCI VM이 먼저 구성되어 있어야 함 (Phase 22 사전 조건 참고)
- GitHub 레포 → Settings → Secrets and variables → Actions에 위 4개 Secret 등록 필요
- OCI VM에 레포가 미리 클론되어 있어야 함 (`git clone <repo-url> $OCI_DEPLOY_PATH`)

---
-->
