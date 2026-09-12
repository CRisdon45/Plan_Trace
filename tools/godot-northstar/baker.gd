extends SceneTree

## Headless Northstar grass bake. Writes PNGs and quits.
## Usage: godot --headless --path tools/godot-northstar --script res://baker.gd -- OUT_DIR [SEED]

const WIDTH := 2048
const HEIGHT := 1434
const STAGE_NAMES := [
	"01-base-wash",
	"02-midtone",
	"03-drying-fronts",
	"04-lifted-texture",
	"05-final-grass",
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
		print("Godot Northstar grass bake: %s" % output_dir)
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

func _apply_study_lot(mat: ShaderMaterial) -> void:
	# L-lawn matching the concave TURF clip in GroundMaterialTest, plus a house
	# and path hole. Plugin contract: lot + holes in UV, seed, material.
	var lot := PackedVector2Array([
		Vector2(0.08, 0.15),
		Vector2(0.91, 0.15),
		Vector2(0.91, 0.84),
		Vector2(0.47, 0.84),
		Vector2(0.47, 0.52),
		Vector2(0.08, 0.52),
	])
	var house := PackedVector2Array([
		Vector2(0.16, 0.22),
		Vector2(0.40, 0.22),
		Vector2(0.40, 0.44),
		Vector2(0.16, 0.44),
	])
	var path := PackedVector2Array([
		Vector2(0.62, 0.52),
		Vector2(0.70, 0.52),
		Vector2(0.70, 0.84),
		Vector2(0.62, 0.84),
	])
	mat.set_shader_parameter("u_lot", lot)
	mat.set_shader_parameter("u_lot_count", lot.size())
	mat.set_shader_parameter("u_hole_a", house)
	mat.set_shader_parameter("u_hole_a_count", house.size())
	mat.set_shader_parameter("u_hole_b", path)
	mat.set_shader_parameter("u_hole_b_count", path.size())

func _write_crops() -> void:
	var image := Image.new()
	if image.load("%s/grass.png" % output_dir) != OK:
		return
	# Lawn interior, house/lawn junction, L inner corner (the clip money shot).
	_crop(image, Vector2i(int(0.68 * WIDTH) - 360, int(0.36 * HEIGHT) - 360), "crop-center.png")
	_crop(image, Vector2i(int(0.28 * WIDTH) - 360, int(0.28 * HEIGHT) - 360), "crop-left.png")
	_crop(image, Vector2i(int(0.47 * WIDTH) - 360, int(0.52 * HEIGHT) - 360), "crop-right.png")

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
