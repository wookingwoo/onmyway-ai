"""Deterministic original motion graphics; genuine app pixels stay separate."""
from functools import lru_cache
import math

from PIL import Image, ImageDraw, ImageFont, ImageOps

W, H = 1080, 1920
INK, WHITE, MUTED = "#0A1B29", "#F6F3E9", "#B8C9D3"
LIME, ORANGE, PANEL = "#DFFF75", "#FFAB83", "#183444"
TRANSITIONS = {"cut", "swipe", "wipe", "zoom", "split"}


@lru_cache(maxsize=160)
def get_font(path, size):
    return ImageFont.truetype(str(path), size)


def label(value, size, font_path, color=WHITE, width=808):
    font = get_font(font_path, size)
    probe = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    while probe.textlength(value, font=font) > width and size > 24:
        size -= 1
        font = get_font(font_path, size)
    box = probe.textbbox((0, 0), value, font=font, anchor="lt")
    if box[2] > width:
        raise ValueError(f"Motion text exceeds safe width: {value}")
    tile = Image.new("RGBA", (max(1, box[2] + 4), max(1, box[3] + 8)))
    ImageDraw.Draw(tile).text((0, 0), value, font=font, fill=color, anchor="lt")
    return tile


def paste_at(canvas, tile, xy):
    canvas.paste(tile, (round(xy[0]), round(xy[1])), tile if tile.mode == "RGBA" else None)


def card(size, fill=PANEL, outline="#365666", radius=28):
    tile = Image.new("RGBA", size)
    ImageDraw.Draw(tile).rounded_rectangle((1, 1, size[0] - 2, size[1] - 2), radius,
                                           fill=fill, outline=outline, width=3)
    return tile


def badge(value, bold, color=LIME):
    words = label(value, 34, bold, INK, 740)
    tile = card((words.width + 44, words.height + 28), color, color, 18)
    paste_at(tile, words, (22, 14))
    return tile


def milk_character(size, bold):
    tile = Image.new("RGBA", (400, 520))
    d = ImageDraw.Draw(tile)
    # Entirely original drawing; no borrowed meme image or branded packaging.
    d.polygon([(55, 118), (145, 40), (285, 40), (355, 118), (355, 466), (55, 466)], fill=WHITE)
    d.line([(55, 118), (145, 40), (285, 40), (355, 118), (355, 466), (55, 466), (55, 118)], fill=LIME, width=8)
    d.line((55, 118, 355, 118), fill=INK, width=5)
    d.ellipse((120, 194, 151, 239), fill=INK)
    d.ellipse((258, 194, 289, 239), fill=INK)
    d.arc((166, 234, 243, 287), 0, 180, fill=INK, width=7)
    paste_at(tile, label("우유", 61, bold, INK), (139, 332))
    d.line((70, 466, 43, 506), fill=LIME, width=9)
    d.line((325, 466, 355, 506), fill=LIME, width=9)
    return tile.resize(size, Image.Resampling.LANCZOS)


def spring(p):
    p = max(0.0, min(1.0, p))
    return 1 + 2.70158 * (p - 1) ** 3 + 1.70158 * (p - 1) ** 2


def add(layers, tile, x, y, delay=0, motion="pop", exit_at=None):
    layers.append({"tile": tile, "x": x, "y": y, "delay": delay,
                   "motion": motion, "exit_at": exit_at})


def task_tile(title, note, regular, bold, color=LIME):
    tile = card((790, 220), outline=color)
    paste_at(tile, label(title, 58, bold, WHITE, 710), (30, 30))
    paste_at(tile, label(note, 35, regular, color, 710), (30, 126))
    return tile


def prepare_scene(scene, regular, bold):
    """Prepare small cached layers once; render_frame only moves/composites them."""
    regular, bold = str(regular), str(bold)
    gradient = Image.linear_gradient("L").resize((W, H))
    base = ImageOps.colorize(gradient, "#091B2B", "#19414B").convert("RGB")
    d = ImageDraw.Draw(base)
    for x in range(-800, 1500, 230):
        d.line((x, H, x + 870, 0), fill="#203C4A", width=2)
    d.arc((330, 530, 1370, 1570), 100, 285, fill="#345563", width=4)
    paste_at(base, label("ONMYWAY AI", 29, bold), (72, 104))
    paste_at(base, label("PLEOS CONNECT", 23, regular, MUTED, 240), (640, 109))
    d.line((72, 174, 880, 174), fill="#3A5B69", width=3)
    accent = ORANGE if scene.get("warning") else LIME
    paste_at(base, label(scene["chapter"], 32, bold, accent), (72, 216))
    layers = []
    for n, line in enumerate(scene["headline"]):
        add(layers, label(line, 94, bold, accent if n == 1 else WHITE), 72, 300 + n * 116,
            delay=0 if n == 0 else .12, motion="headline")
    if scene.get("sticker"):
        add(layers, badge(scene["sticker"], bold, accent), 80, 542, delay=.26, motion="stamp")
    art = scene["art"]
    if art in {"quest", "close"}:
        for n, (title, note) in enumerate([("약 구매", "필수"), ("세탁물 찾기", "필수 · 수령 마감"), ("우유 구매", "선택")]):
            tile = card((780, 150), outline=LIME if n < 2 else "#627B88")
            paste_at(tile, label(title, 48, bold), (28, 27))
            paste_at(tile, label(note, 31, regular, LIME if n < 2 else MUTED), (410, 42))
            add(layers, tile, 86, 700 + n * 180, .10 + n * .18, "slide")
    elif art == "clock":
        add(layers, label("19:00", 198, bold, LIME), 72, 710, .08, "stamp")
        add(layers, badge("집 마감 / 남은 용무 3개", bold), 80, 1050, .35, "slide")
    elif art == "required":
        add(layers, task_tile("약은 꼭", "필수 · 10분 머무름", regular, bold), 80, 700, .04)
        add(layers, task_tile("세탁물도 꼭", "18:30 전 수령 완료", regular, bold, ORANGE), 80, 960, .3)
    elif art in {"milk", "milk-out"}:
        add(layers, milk_character((335, 435), bold), 300, 810, .04,
            "wobble", exit_at=.85 if art == "milk-out" else None)
        speech = card((780, 133), WHITE, WHITE)
        phrase = "나도 데려가…" if art == "milk" else "ㅇㅋ 다음에"
        paste_at(speech, label("우유: " + phrase, 53, bold, INK), (32, 34))
        add(layers, speech, 80, 655, .20, "slide")
        if art == "milk-out":
            add(layers, badge("필수 2개 유지 · 선택 1개 제외", bold), 80, 1190, .35, "stamp")
    elif art == "question":
        add(layers, label("가능?", 185, bold, LIME), 180, 750, .08, "stamp")
        add(layers, badge("이동만 세면 놓치는 시간", bold), 80, 1080, .35, "slide")
    elif art == "deadline":
        add(layers, label("18:20", 184, bold, ORANGE), 72, 755, .02, "shake")
        add(layers, badge("필수 + 귀가, 지금은 불가능", bold, ORANGE), 80, 1080, .22, "stamp")
    elif art in {"footage", "calculate"}:
        plate = card((824, 650), "#0B1C28", "#4B6573")
        paste_at(plate, label("실제 앱 확대 편집 · 1배속", 27, regular, MUTED), (22, 18))
        paste_at(plate, label(scene["focus"], 43, bold, WHITE, 770), (22, 76))
        # The moving frame is pasted into this reserved window by render_frame.
        add(layers, plate, 72, 635, 0, "plate")
        if art == "calculate":
            for n, word in enumerate(["이동", "대기", "용무", "귀가"]):
                tile = badge(word, bold)
                add(layers, tile, 80 + n * 194, 550, .12 + .16 * n, "pop")
    elif art in {"payoff", "brand"}:
        if art == "payoff":
            for n, value in enumerate(["필수 완료", "남은 일정 확인", "마지막은 집으로"]):
                add(layers, task_tile(value, "예시 A · 완료 흐름", regular, bold), 80,
                    660 + n * 202, .1 + .18 * n, "slide")
        else:
            add(layers, label("OnMyWay", 122, bold), 76, 710, .08, "pop")
            add(layers, label("AI", 118, bold, LIME), 80, 850, .25, "stamp")
            route = Image.new("RGBA", (808, 155))
            rd = ImageDraw.Draw(route)
            rd.line((25, 30, 760, 30), fill="#657F88", width=5)
            for n, name in enumerate(["약", "세탁물", "우유", "집"]):
                x = 25 + n * 245
                rd.ellipse((x - 13, 17, x + 13, 43), fill=LIME)
                paste_at(route, label(name, 35, bold, WHITE, 160), (max(0, x - 33), 67))
            add(layers, route, 80, 1060, .38, "slide")
            add(layers, badge("당신의 퇴근길 퀘스트는?", bold), 80, 1250, .65, "stamp")
    else:
        raise ValueError(f"Unknown motion artwork: {art}")
    if scene.get("note"):
        paste_at(base, label(scene["note"], 31, bold, accent), (78, 1314))
    # Caption/disclosure are always drawn last, stationary across every transition.
    footer = Image.new("RGBA", (W, H - 1380))
    for n, line in enumerate(scene["caption"].splitlines()):
        paste_at(footer, label(line, 43, bold), (78, 30 + n * 62))
    fd = ImageDraw.Draw(footer)
    fd.line((78, 190, 880, 190), fill="#42606C", width=2)
    paste_at(footer, label("예시 해석·가상 장소·시간 / 안내·도착은 모의", 27, regular, MUTED), (78, 218))
    paste_at(footer, label("실제 AI·도로·내비 결과 아님", 27, regular, MUTED), (78, 264))
    return {"scene": scene, "base": base, "layers": layers, "footer": footer}


def draw_layer(canvas, layer, t):
    age = t - layer["delay"]
    if age < 0:
        return
    p = min(1, age / .34)
    tile, x, y = layer["tile"], layer["x"], layer["y"]
    kind = layer["motion"]
    if kind == "headline":
        # Text is already present at age zero; only a small upward settling motion.
        y += 22 * (1 - p) ** 3
    elif kind == "slide":
        x += 120 * (1 - p) ** 3
    elif kind in {"pop", "stamp"}:
        scale = .78 + .22 * spring(p)
        resized = tile.resize((max(1, round(tile.width * scale)), max(1, round(tile.height * scale))), Image.Resampling.BICUBIC)
        x += (tile.width - resized.width) / 2
        y += (tile.height - resized.height) / 2
        tile = resized
    elif kind == "wobble":
        angle = 7 * math.sin(age * 9) * math.exp(-age * 1.9)
        rotated = tile.rotate(angle, Image.Resampling.BICUBIC, expand=True)
        x += (tile.width - rotated.width) / 2
        y += (tile.height - rotated.height) / 2
        tile = rotated
    elif kind == "shake":
        x += 10 * math.sin(age * 45) * math.exp(-age * 8)
    if layer["exit_at"] is not None and t > layer["exit_at"]:
        progress = min(1, (t - layer["exit_at"]) / .6)
        x += 1050 * progress ** 2
        y -= 90 * math.sin(progress * math.pi)
    paste_at(canvas, tile, (x, y))


def render_frame(layout, t, capture=None, previous=None):
    """Render editorial motion without changing captions or the disclosure layer."""
    canvas = layout["base"].copy()
    scene = layout["scene"]
    for layer in layout["layers"]:
        draw_layer(canvas, layer, t)
    if capture is not None:
        paste_at(canvas, capture, (484 - capture.width / 2, 1038 - capture.height / 2))
    # Small route cursor is in addition to actual animated cards/characters/titles.
    ImageDraw.Draw(canvas).ellipse((72 + 808 * min(1, t / scene["duration"]) - 5, 168,
                                   72 + 808 * min(1, t / scene["duration"]) + 5, 178), fill=LIME)
    transition = scene.get("transition", {"kind": "cut", "frames": 0})
    span = transition["frames"] / 30
    if previous is not None and span and t < span:
        p = min(1, t / span)
        kind = transition["kind"]
        # Only the story area moves. Header/captions/disclosure are never swept away.
        box = (0, 265, W, 1380)
        fresh, old = canvas.crop(box), previous.crop(box)
        if kind == "wipe":
            edge = round(W * (1 - p))
            fresh.paste(old.crop((0, 0, edge, old.height)), (0, 0))
        elif kind == "swipe":
            shift = round(W * (1 - p) ** 2)
            mixed = old.copy()
            mixed.paste(fresh, (shift, 0))
            fresh = mixed
        elif kind == "split":
            shift = round(W / 2 * p)
            fresh.paste(old.crop((0, 0, W // 2, old.height)), (-shift, 0))
            fresh.paste(old.crop((W // 2, 0, W, old.height)), (W // 2 + shift, 0))
        elif kind == "zoom":
            scale = 1.05 - .05 * p
            bigger = fresh.resize((round(W * scale), round(fresh.height * scale)), Image.Resampling.BICUBIC)
            left, top = (bigger.width - W) // 2, (bigger.height - fresh.height) // 2
            fresh = bigger.crop((left, top, left + W, top + fresh.height))
        canvas.paste(fresh, (0, 265))
    paste_at(canvas, layout["footer"], (0, 1380))
    return canvas
