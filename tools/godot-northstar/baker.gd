extends SceneTree

## Headless Northstar grass bake. Writes PNGs and quits.
## Usage: godot --headless --path tools/godot-northstar --script res://baker.gd -- OUT_DIR [SEED]

const WIDTH := 2048
const HEIGHT := 1434
const STAGE_NAMES := [
	"01-linework",
	"02-first-wash",
	"03-second-wash",
	"04-shadow-planting",
	"05-final-plan",
]

var output_dir := "user://"
var seed_value := 0.37
var frame := 0
var viewport: SubViewport
var rect: ColorRect
var material: ShaderMaterial
var pending_stage := 1
var capturing := false

func _initialize() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		output_dir = args[0]
	if args.size() > 1:
		seed_value = _seed_from_id(args[1])
	DirAccess.make_dir_recursive_absolute(output_dir)

	viewport = SubViewport.new()
	viewport.size = Vector2i(WIDTH, HEIGHT)
	viewport.disable_3d = true
	viewport.transparent_bg = false
	viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	viewport.render_target_clear_mode = SubViewport.CLEAR_MODE_ALWAYS
	root.add_child(viewport)

	material = ShaderMaterial.new()
	material.shader = load("res://watercolor_grass.gdshader")
	material.set_shader_parameter("u_size", Vector2(WIDTH, HEIGHT))
	material.set_shader_parameter("u_seed", seed_value)
	material.set_shader_parameter("u_stage", 1)
	_apply_study_lot(material)

	rect = ColorRect.new()
	rect.color = Color.WHITE
	rect.size = Vector2(WIDTH, HEIGHT)
	rect.material = material
	viewport.add_child(rect)
	pending_stage = 1
	capturing = false

func _process(_dt: float) -> bool:
	frame += 1
	if frame < 4:
		return false
	if capturing:
		return false
	capturing = true
	_capture_stage(pending_stage)
	pending_stage += 1
	if pending_stage > 5:
		_write_crops()
		_write_sheet()
		print("Godot Northstar plan bake: %s" % output_dir)
		quit()
		return true
	material.set_shader_parameter("u_stage", pending_stage)
	frame = 0
	capturing = false
	return false

func _capture_stage(stage: int) -> void:
	var tex := viewport.get_texture()
	if tex == null:
		push_error("No viewport texture")
		quit(1)
		return
	var image := tex.get_image()
	if image == null:
		push_error("Viewport image was empty")
		quit(1)
		return
	var path := "%s/%s.png" % [output_dir, STAGE_NAMES[stage - 1]]
	var err := image.save_png(path)
	if err != OK:
		push_error("Failed to save %s (%s)" % [path, err])
		quit(1)
		return
	if stage == 5:
		image.save_png("%s/grass.png" % output_dir)
		image.save_png("%s/plan.png" % output_dir)

func _apply_study_lot(mat: ShaderMaterial) -> void:
	# Backyard study: left lawn strip, residence, L-pool, paving is the remainder.
	var site := PackedVector2Array([
		Vector2(0.10, 0.10),
		Vector2(0.90, 0.10),
		Vector2(0.90, 0.88),
		Vector2(0.10, 0.88),
	])
	var house := PackedVector2Array([
		Vector2(0.28, 0.52),
		Vector2(0.72, 0.52),
		Vector2(0.72, 0.86),
		Vector2(0.28, 0.86),
	])
	var lawn := PackedVector2Array([
		Vector2(0.10, 0.10),
		Vector2(0.24, 0.10),
		Vector2(0.24, 0.86),
		Vector2(0.10, 0.86),
	])
	var pool := PackedVector2Array([
		Vector2(0.34, 0.16),
		Vector2(0.70, 0.16),
		Vector2(0.70, 0.38),
		Vector2(0.58, 0.38),
		Vector2(0.58, 0.46),
		Vector2(0.34, 0.46),
	])
	mat.set_shader_parameter("u_site", site)
	mat.set_shader_parameter("u_site_count", site.size())
	mat.set_shader_parameter("u_house", house)
	mat.set_shader_parameter("u_house_count", house.size())
	mat.set_shader_parameter("u_lawn", lawn)
	mat.set_shader_parameter("u_lawn_count", lawn.size())
	mat.set_shader_parameter("u_pool", pool)
	mat.set_shader_parameter("u_pool_count", pool.size())

func _write_crops() -> void:
	var image := Image.new()
	if image.load("%s/plan.png" % output_dir) != OK:
		return
	_crop(image, Vector2i(int(0.50 * WIDTH) - 360, int(0.28 * HEIGHT) - 360), "crop-center.png")
	_crop(image, Vector2i(int(0.17 * WIDTH) - 360, int(0.40 * HEIGHT) - 360), "crop-left.png")
	_crop(image, Vector2i(int(0.62 * WIDTH) - 360, int(0.40 * HEIGHT) - 360), "crop-right.png")

func _write_sheet() -> void:
	var cell := Vector2i(380, 266)
	var sheet := Image.create(25 + 5 * (cell.x + 20), 92 + cell.y + 50, false, Image.FORMAT_RGBA8)
	sheet.fill(Color(0.976, 0.969, 0.937, 1))
	for i in 5:
		var stage := Image.new()
		if stage.load("%s/%s.png" % [output_dir, STAGE_NAMES[i]]) != OK:
			continue
		stage.resize(cell.x, cell.y, Image.INTERPOLATE_LANCZOS)
		sheet.blit_rect(stage, Rect2i(Vector2i.ZERO, cell), Vector2i(25 + i * (cell.x + 20), 92))
	sheet.save_png("%s/grass-layer-progression.png" % output_dir)

func _crop(image: Image, origin: Vector2i, name: String) -> void:
	var region := Rect2i(origin, Vector2i(720, 720))
	region.position.x = clampi(region.position.x, 0, image.get_width() - 720)
	region.position.y = clampi(region.position.y, 0, image.get_height() - 720)
	var crop := image.get_region(region)
	crop.save_png("%s/%s" % [output_dir, name])

func _seed_from_id(id: String) -> float:
	var h := 2166136261
	for i in id.length():
		h = (h ^ id.unicode_at(i)) * 16777619
		h = h & 0x7fffffff
	return float(h % 1000000) / 1000000.0
