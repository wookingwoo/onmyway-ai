# Official form working draft contract

Reference: `official-template.docx`, SHA-256 `07be893d58b0e96ab7788f0d189d6829f0e59a2cee4bc18cb90ec37eeda2f5fd`. Word COM read-only export produced `qa-reference.pdf` and five Poppler page PNGs; all five pages inspected. Single A4 portrait section, 7560310 by 10692130 EMU; margins top 593725, bottom 575945, left/right 647700 EMU. Preserve section/page properties, template fonts, spacing, headings, colors, tables and legal material.

Body sequence: operational application; anonymous project introduction; privacy terms; individual consent/signature. Source page 2 contains project 1–7; source page 3 contains project 8. Larger project text may expand pagination, which the source explicitly permits. Operations and consent sections must remain structurally unchanged.

Editable slots: `word/document.xml` body top-level tables 1–8, each a single-cell row and a single inline plain-text content control. Map by visible numbered heading/order, since several source control aliases/tags are stale. Insert text only in the existing first `w:t` without replacing the content control or table. Use wrapping within one paragraph and keep all run/paragraph properties. Preserve table `cantSplit`, minimum (not fixed) height, borders, 110-twip cell padding and 280-twip exact line spacing. `word/document.xml` top-level paragraph 28 is the anonymous project title; replace its text runs while retaining run/paragraph properties.

Preserve-only package parts: every ZIP member except `word/document.xml` and `docProps/core.xml`. All relationships, styles, numbering, headers/footers, opaque/custom parts and embedded objects remain byte-identical. Inside `document.xml`, the eight slot text nodes and anonymous project-title text runs are the only intended changes. Clear core author/last editor and description; keep original legal form and personal-information/consent/signature fields unchanged.

Verification: package-member hash comparison, structural comparison outside editable slots, content-control count/tags retention, geometry retention and all pages of final Word COM PDF export plus Poppler PNGs. The official template remains unchanged. Word opens read-only, macros disabled, link updates disabled, and exports a local PDF without saving the DOCX. No network/publication actions.

Status: working draft. Engineering evidence and entrant identity/consent/signature remain incomplete. A layout pass alone does not make this submission-ready.
