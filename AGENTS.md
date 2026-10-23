# OnMyWay AI development

Keep this a small native Android + FastAPI monorepo. Prefer completing and
verifying the end-to-end errand flow over adding infrastructure or features.

- Korean UI and contest prose; English source, engineering docs and commits.
- User supplies API keys later. Only the server reads ignored environment files.
  Never embed keys in APKs, prompts in screenshots, source or test reports.
- Fixtures are explicitly labelled, independently requested and never a silent
  replacement for a failed live provider.
- Preserve mandatory errands. Count road travel, opening wait and dwell before
  per-errand and final deadlines. Unknown data cannot establish feasibility.
- Native vehicle state fails closed. Require approval for the current fresh plan.
  Arrival is separate from completion; final home navigation is part of the flow.
- Test behavior and relevant boundaries. Run Python tests and Android unit/build
  checks after substantive code changes; record actual evidence in docs/progress.md.
- Keep the official project section and shared assets anonymous. Identity,
  signatures, external access grants, public publication and final email submission
  require user action/authorization. Do not claim unperformed live tests or results.
