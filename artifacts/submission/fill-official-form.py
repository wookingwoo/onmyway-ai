"""Patch anonymous source-form text slots only; preserve unrelated ZIP members."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from lxml import etree as E
import hashlib
import json
import re

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
REFERENCE = HERE / 'official-template.docx'
OUTPUT = HERE / 'OnMyWay_AI_application_WORKING_DRAFT.docx'
EXPECTED = '07be893d58b0e96ab7788f0d189d6829f0e59a2cee4bc18cb90ec37eeda2f5fd'
assert hashlib.sha256(REFERENCE.read_bytes()).hexdigest() == EXPECTED
source = (ROOT / 'docs/submission/application-ko.md').read_text(encoding='utf-8')
chunks = re.split(r'^## [1-8][.] .+\n', source, flags=re.M)[1:]
assert len(chunks) == 8
content = [' '.join(line.removeprefix('- ').strip() for line in part.strip().splitlines() if line.strip()) for part in chunks]
ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
W = '{' + ns['w'] + '}'
parser = E.XMLParser(remove_blank_text=False)
with ZipFile(REFERENCE) as z:
    original = {i.filename: z.read(i.filename) for i in z.infolist()}
    infos = z.infolist()
doc = E.fromstring(original['word/document.xml'], parser)
body = doc.find('w:body', ns)
tables = body.findall('w:tbl', ns)
assert len(tables) == 10
for i, text in enumerate(content, 1):
    texts = tables[i].findall('.//w:sdt/w:sdtContent//w:t', ns)
    assert len(texts) == 1, (i, len(texts))
    texts[0].text = text
    texts[0].set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
title_nodes = body.findall('w:p', ns)[28].findall('.//w:t', ns)
title_nodes[0].text = '앱명 / 프로젝트명: OnMyWay AI'
for text_node in title_nodes[1:]:
    text_node.text = ''
updated = dict(original)
updated['word/document.xml'] = E.tostring(doc, xml_declaration=True, encoding='UTF-8', standalone=True)
core = E.fromstring(original['docProps/core.xml'], parser)
for el in core:
    if E.QName(el).localname in ('creator', 'lastModifiedBy', 'description'):
        el.text = ''
updated['docProps/core.xml'] = E.tostring(core, xml_declaration=True, encoding='UTF-8', standalone=True)
with ZipFile(OUTPUT, 'w', compression=ZIP_DEFLATED) as z:
    for info in infos:
        z.writestr(info, updated[info.filename])
changed = [name for name in original if original[name] != updated[name]]
assert sorted(changed) == ['docProps/core.xml', 'word/document.xml']

# Compare document structure after masking only the explicitly editable text nodes.
baseline = E.fromstring(original['word/document.xml'], parser)
def masked_tree(tree):
    b = tree.find('w:body', ns)
    for table in b.findall('w:tbl', ns)[1:9]:
        for t in table.findall('.//w:sdt/w:sdtContent//w:t', ns):
            t.text = '[SLOT]'
    for t in b.findall('w:p', ns)[28].findall('.//w:t', ns):
        t.text = '[TITLE]'
    return E.tostring(tree, method='c14n')
assert masked_tree(baseline) == masked_tree(E.fromstring(updated['word/document.xml'], parser))
report = {
    'output': OUTPUT.name,
    'reference_sha256': EXPECTED,
    'output_sha256': hashlib.sha256(OUTPUT.read_bytes()).hexdigest(),
    'changed_package_parts': changed,
    'untouched_parts_byte_identical': len(original) - len(changed),
    'document_structure_outside_slots_unchanged': True,
    'personal_information_consent_signature_unchanged': True,
    'anonymous_slots_filled': 8,
    'visual_qa': 'pending',
    'submission_status': 'working_draft_incomplete_evidence_and_identity'
}
(HERE / 'docx-verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
