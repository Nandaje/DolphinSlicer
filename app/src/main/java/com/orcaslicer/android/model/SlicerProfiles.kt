package com.orcaslicer.android.model

enum class GCodeFlavor(val label: String) {
    BAMBU("Bambu / Orca"),
    KLIPPER("Klipper"),
    MARLIN("Marlin 2"),
    PRUSA("Prusa / Marlin"),
    REPRAP("RepRapFirmware")
}

enum class ZHopType(val label: String) {
    AUTO("Auto"),
    NORMAL("Normal"),
    SLOPE("Slope"),
    SPIRAL("Spiral")
}

enum class WallGeneratorType(val label: String) {
    ARACHNE("Arachne (Variable Width)"),
    CLASSIC("Classic")
}

enum class ScarfSeamType(val label: String) {
    NONE("None"),
    CONTOUR("Contour"),
    CONTOUR_AND_HOLE("Contour and Hole")
}

enum class IroningType(val label: String) {
    NONE("No Ironing"),
    ALL_TOP("All Top Surfaces"),
    TOPMOST_ONLY("Topmost Surface Only")
}

enum class SurfacePattern(val label: String) {
    MONOTONIC("Monotonic"),
    MONOTONIC_LINE("Monotonic Line"),
    RECTILINEAR("Rectilinear"),
    CONCENTRIC("Concentric"),
    HILBERT_CURVE("Hilbert Curve"),
    ARCHIMEDEAN("Archimedean Chords")
}

enum class InfillPattern(val label: String) {
    GYROID("Gyroid"),
    GRID("Grid"),
    HONEYCOMB("Honeycomb"),
    ADAPTIVE_CUBIC("Adaptive Cubic"),
    TRIANGLES("Triangles"),
    TRI_HEXAGON("Tri-Hexagon"),
    LINES("Rectilinear / Lines"),
    CONCENTRIC("Concentric"),
    CROSS_HATCH("Cross Hatch"),
    LIGHTNING("Lightning")
}

enum class SupportType(val label: String) {
    NORMAL_GRID("Normal (Auto)"),
    TREE_ORGANIC("Tree (Auto)"),
    NORMAL_MANUAL("Normal (Manual)"),
    TREE_MANUAL("Tree (Manual)")
}

enum class SupportStyle(val label: String) {
    DEFAULT("Default"),
    GRID("Grid"),
    SNUG("Snug"),
    ORGANIC("Tree Organic"),
    TREE_SLIM("Tree Slim"),
    TREE_STRONG("Tree Strong"),
    TREE_HYBRID("Tree Hybrid")
}

enum class BrimType(val label: String) {
    AUTO("Auto"),
    OUTER_BRIM("Outer Brim Only"),
    INNER_BRIM("Inner Brim Only"),
    OUTER_AND_INNER("Outer and Inner Brim"),
    SKIRT("Skirt Loop"),
    NONE("No-brim")
}

enum class SeamPosition(val label: String) {
    ALIGNED("Aligned"),
    NEAREST("Nearest"),
    BACK("Back"),
    RANDOM("Random")
}

enum class FuzzySkinType(val label: String) {
    NONE("None"),
    CONTOUR("Contour"),
    CONTOUR_AND_HOLE("Contour and Hole"),
    ALL_WALLS("All Walls")
}

enum class PrintSequence(val label: String) {
    BY_LAYER("By layer"),
    BY_OBJECT("By object")
}

enum class WallsPrintingOrder(val label: String) {
    INNER_OUTER("Inner/Outer"),
    OUTER_INNER("Outer/Inner"),
    INNER_OUTER_INNER("Inner/Outer/Inner")
}

enum class WallLoopDirection(val label: String) {
    AUTO("Auto"),
    COUNTER_CLOCKWISE("Counter clockwise"),
    CLOCKWISE("Clockwise")
}

enum class TopOneWallMode(val label: String) {
    NOT_APPLIED("Not applied"),
    TOPMOST_SURFACE("Topmost surface"),
    ALL_TOP_SURFACES("All top surfaces")
}

enum class ExtraBridgeLayersMode(val label: String) {
    DISABLED("Disabled"),
    APPLY_TO_ALL("Apply to all"),
    EXTERNAL_BRIDGE_ONLY("External bridge only")
}

enum class FilterSmallBridgesMode(val label: String) {
    FILTER("Filter"),
    NO_FILTER("No filter"),
    LIMITED("Limited")
}

enum class BridgeCounterboreMode(val label: String) {
    NONE("None"),
    PARTIALLY_BRIDGED("Partially bridged"),
    SACRIFICIAL_LAYER("Sacrificial layer")
}

enum class GapFillPlacement(val label: String) {
    NOWHERE("Nowhere"),
    EVERYWHERE("Everywhere"),
    TOP_AND_BOTTOM("Top and bottom surfaces")
}

enum class VerticalShellThicknessMode(val label: String) {
    ALL("All"),
    CRITICAL_ONLY("Critical only"),
    MODERATE("Moderate"),
    NONE("None")
}

enum class SupportBasePattern(val label: String) {
    RECTILINEAR("Rectilinear"),
    RECTILINEAR_GRID("Rectilinear grid"),
    HONEYCOMB("Honeycomb"),
    LIGHTNING("Lightning"),
    HOLLOW("Hollow")
}

enum class SupportInterfacePattern(val label: String) {
    DEFAULT("Default"),
    RECTILINEAR("Rectilinear"),
    CONCENTRIC("Concentric"),
    RECTILINEAR_INTERLACED("Rectilinear interlaced"),
    GRID("Grid")
}

enum class WipeTowerWallType(val label: String) {
    RIB("Rib"),
    CONE("Cone"),
    PLAIN("Plain")
}

enum class SkirtType(val label: String) {
    COMBINED("Combined"),
    PER_OBJECT("Per object")
}

enum class DraftShieldMode(val label: String) {
    DISABLED("Disabled"),
    ENABLED("Enabled"),
    LIMITED("Limited")
}

enum class SlicingModeType(val label: String) {
    REGULAR("Regular"),
    EVEN_ODD("Even-odd"),
    CLOSE_HOLES("Close holes")
}

enum class IntraLayerOrder(val label: String) {
    DEFAULT("Default"),
    AS_ARRANGED("As arranged")
}

enum class CombingMode(val label: String) {
    OFF("Off"),
    ALL("All"),
    NOT_IN_SKIN("Not in Skin"),
    NOT_ON_OUTER_SURFACE("Not on Outer Surface"),
    WITHIN_INFILL("Within Infill")
}

data class PrinterProfile(
    val id: String,
    val vendor: String = "Generic",
    val name: String,
    val bedWidthX: Float,
    val bedDepthY: Float,
    val maxPrintHeightZ: Float,
    val nozzleDiameterMm: Float = 0.4f,
    val nozzleType: String = "Hardened Steel",
    val gcodeFlavor: GCodeFlavor = GCodeFlavor.BAMBU,
    val maxSpeedXYMmS: Float = 500f,
    val maxSpeedZMmS: Float = 20f,
    val maxAccelerationMmS2: Float = 10000f,
    val retractionLengthMm: Float = 0.8f,
    val retractionSpeedMmS: Float = 35f,
    val zHopMm: Float = 0.4f,
    val zHopType: ZHopType = ZHopType.SPIRAL,
    val wipeOnRetract: Boolean = true,
    val powerConsumptionWatts: Float = 150f,
    val electricityRatePerKwh: Float = 0.14f,
    val hourlyMachineRate: Float = 0.35f,
    val startGcode: String = """
        G90 ; use absolute coordinates
        M83 ; extruder relative mode
        M140 S[bed_temperature_initial_layer] ; set bed temp
        M104 S[nozzle_temperature_initial_layer] ; set nozzle temp
        G28 ; home all axes
        M190 S[bed_temperature_initial_layer] ; wait for bed temp
        M109 S[nozzle_temperature_initial_layer] ; wait for nozzle temp
        G92 E0
        G1 Z2.0 F3000
        G1 X5.0 Y10.0 Z0.28 F5000.0 ; move to prime line start
        G1 X5.0 Y140.0 Z0.28 F1500.0 E12 ; draw first prime line
        G1 X5.4 Y140.0 Z0.28 F5000.0 ; shift right
        G1 X5.4 Y10.0 Z0.28 F1500.0 E12 ; draw second prime line
        G92 E0
    """.trimIndent(),
    val endGcode: String = """
        G91 ; relative positioning
        G1 E-2 F2700 ; retract filament
        G1 Z5 F3000 ; lift nozzle
        G90 ; absolute positioning
        G1 X0 Y[bed_depth] F6000 ; present print
        M106 S0 ; turn off cooling fan
        M104 S0 ; turn off hotend
        M140 S0 ; turn off bed
        M84 ; disable motors
    """.trimIndent()
)

data class FilamentProfile(
    val id: String,
    val vendor: String = "Generic",
    val name: String,
    val materialType: String,
    val colorHex: Long,
    val diameterMm: Float = 1.75f,
    val densityGPerCm3: Float = 1.24f,
    val spoolWeightGrams: Float = 1000f,
    val costPerKgUsd: Float = 19.99f,
    val currencySymbol: String = "$",
    val markupMultiplier: Float = 1.50f,
    val nozzleTempInitialC: Int = 220,
    val nozzleTempOtherC: Int = 215,
    val bedTempInitialC: Int = 60,
    val bedTempOtherC: Int = 60,
    val chamberTempC: Int = 0,
    val flowRatio: Float = 0.98f,
    val maxVolumetricSpeedMm3S: Float = 21.0f,
    val enablePressureAdvance: Boolean = true,
    val pressureAdvance: Float = 0.036f,
    val retractionLengthMm: Float = 0.8f,
    val retractionSpeedMmS: Float = 35f,
    val fanMinSpeedPercent: Int = 80,
    val fanSpeedPercent: Int = 100,
    val auxFanSpeedPercent: Int = 70,
    val closeFanFirstLayers: Int = 1,
    val slowDownForLayerCooling: Boolean = true,
    val minLayerTimeSeconds: Int = 8
) {
    val costPerGram: Float
        get() = costPerKgUsd / 1000f

    val spoolTotalCost: Float
        get() = (spoolWeightGrams / 1000f) * costPerKgUsd
}

data class ProcessProfile(
    val id: String,
    val name: String,
    // 1. Quality
    val layerHeightMm: Float = 0.20f,
    val initialLayerHeightMm: Float = 0.20f,
    val lineWidthMm: Float = 0.42f,
    val initialLayerLineWidthMm: Float = 0.50f,
    val outerWallLineWidthMm: Float = 0.42f,
    val innerWallLineWidthMm: Float = 0.45f,
    val topSurfaceLineWidthMm: Float = 0.42f,
    val sparseInfillLineWidthMm: Float = 0.45f,
    val internalSolidLineWidthMm: Float = 0.42f,
    val supportLineWidthMm: Float = 0.42f,
    val bridgeLineWidthPercent: Int = 100,
    val seamPosition: SeamPosition = SeamPosition.ALIGNED,
    val staggeredInnerSeams: Boolean = false,
    val scarfJointSeam: ScarfSeamType = ScarfSeamType.NONE,
    val seamGapPercent: Int = 10,
    val roleBaseWipeSpeed: Boolean = true,
    val wipeSpeedPercent: Int = 80,
    val wipeOnLoops: Boolean = false,
    val wipeBeforeExternalLoop: Boolean = false,
    val resolutionMm: Float = 0.012f,
    val sliceGapClosingRadiusMm: Float = 0.049f,
    val arcFittingG2G3: Boolean = false,
    val xyHoleCompensationMm: Float = 0.0f,
    val xyContourCompensationMm: Float = 0.0f,
    val elephantFootCompensationMm: Float = 0.10f,
    val elephantFootLayersDensityPercent: Int = 100,
    val elephantFootCompensationLayers: Int = 1,
    val preciseWall: Boolean = false,
    val preciseZHeight: Boolean = false,
    val convertHolesToPolyholes: Boolean = false,
    val ironingType: IroningType = IroningType.NONE,
    val ironingSpeedMmS: Float = 30f,
    val ironingFlowPercent: Int = 10,
    val zCompoundingEnabled: Boolean = false,
    val wallGenerator: WallGeneratorType = WallGeneratorType.ARACHNE,
    val wallTransitionThresholdAngleDeg: Int = 10,
    val wallTransitionFilterMarginPercent: Int = 25,
    val wallTransitionLengthPercent: Int = 100,
    val wallDistributionCount: Int = 1,
    val firstLayerMinWallWidthPercent: Int = 85,
    val minWallWidthPercent: Int = 85,
    val minFeatureSizePercent: Int = 25,
    val minWallLengthMm: Float = 0.50f,
    val minWallVolumeMm3: Float = 0.10f,
    val maxWallDeviationMm: Float = 0.025f,
    val wallsPrintingOrder: WallsPrintingOrder = WallsPrintingOrder.INNER_OUTER,
    val printInfillFirst: Boolean = false,
    val wallLoopDirection: WallLoopDirection = WallLoopDirection.COUNTER_CLOCKWISE,
    val topSurfaceFlowRatio: Float = 1.0f,
    val bottomSurfaceFlowRatio: Float = 1.0f,
    val setOtherFlowRatios: Boolean = false,
    val onlyOneWallFirstLayer: Boolean = false,
    val onlyOneWallOnTop: Boolean = true,
    val topOneWallMode: TopOneWallMode = TopOneWallMode.ALL_TOP_SURFACES,
    val avoidCrossingWalls: Boolean = false,
    val avoidCrossingWallsMaxDetourMm: Float = 0.0f,
    val smallAreaFlowCompensation: Boolean = false,
    val bridgeFlowRatio: Float = 0.95f,
    val internalBridgeFlowRatio: Float = 1.0f,
    val externalBridgeDensityPercent: Int = 100,
    val internalBridgeDensityPercent: Int = 100,
    val thickExternalBridges: Boolean = false,
    val thickInternalBridges: Boolean = false,
    val extraBridgeLayersMode: ExtraBridgeLayersMode = ExtraBridgeLayersMode.DISABLED,
    val filterOutSmallInternalBridges: FilterSmallBridgesMode = FilterSmallBridgesMode.FILTER,
    val bridgeCounterboreHoles: BridgeCounterboreMode = BridgeCounterboreMode.NONE,
    val detectOverhangWalls: Boolean = true,
    val makeOverhangsPrintable: Boolean = false,
    val extraPerimetersOnOverhangs: Boolean = false,
    val reverseOnEven: Boolean = false,

    // 2. Strength
    val wallLoops: Int = 2,
    val alternateExtraWall: Boolean = false,
    val detectThinWalls: Boolean = false,
    val topShellLayers: Int = 4,
    val topShellThicknessMm: Float = 0.8f,
    val topSurfaceDensityPercent: Int = 100,
    val topSurfacePattern: SurfacePattern = SurfacePattern.MONOTONIC,
    val bottomShellLayers: Int = 3,
    val bottomShellThicknessMm: Float = 0.0f,
    val bottomSurfaceDensityPercent: Int = 100,
    val bottomSurfacePattern: SurfacePattern = SurfacePattern.MONOTONIC,
    val topBottomSolidInfillWallOverlapPercent: Int = 25,
    val sparseInfillDensityPercent: Int = 15,
    val fillMultiline: Int = 1,
    val sparseInfillPattern: InfillPattern = InfillPattern.CROSS_HATCH,
    val infillDirectionDeg: Int = 45,
    val sparseInfillRotationTemplate: String = "",
    val maxInfillAnchorLengthMm: Float = 20f,
    val sparseInfillAnchorLengthPercent: Int = 400,
    val internalSolidInfillPattern: SurfacePattern = SurfacePattern.MONOTONIC,
    val solidInfillDirectionDeg: Int = 45,
    val solidInfillRotationTemplate: String = "",
    val applyGapFill: GapFillPlacement = GapFillPlacement.NOWHERE,
    val filterOutTinyGapsMm: Float = 0f,
    val infillWallOverlapPercent: Int = 25,
    val alignInfillDirectionToModel: Boolean = false,
    val infillSolidLayersInterval: Int = 0,
    val externalBridgeInfillDirectionDeg: Int = 0,
    val internalBridgeInfillDirectionDeg: Int = 0,
    val relativeBridgeAngle: Boolean = true,
    val minSparseInfillThresholdMm2: Float = 15f,
    val infillCombination: Boolean = false,
    val detectNarrowInternalSolidInfill: Boolean = true,
    val ensureVerticalShellThickness: VerticalShellThicknessMode = VerticalShellThicknessMode.ALL,

    // 3. Speed
    val initialLayerSpeedMmS: Float = 50f,
    val initialLayerInfillSpeedMmS: Float = 105f,
    val initialLayerTravelSpeedPercent: Int = 100,
    val numberOfSlowLayers: Int = 0,
    val outerWallSpeedMmS: Float = 120f,
    val innerWallSpeedMmS: Float = 200f,
    val smallPerimeterSpeedPercent: Int = 50,
    val smallPerimeterThresholdMm: Float = 0f,
    val sparseInfillSpeedMmS: Float = 250f,
    val internalSolidInfillSpeedMmS: Float = 220f,
    val topSurfaceSpeedMmS: Float = 100f,
    val gapInfillSpeedMmS: Float = 180f,
    val supportSpeedMmS: Float = 150f,
    val supportInterfaceSpeedMmS: Float = 80f,
    val slowDownForOverhangs: Boolean = true,
    val slowDownForCurledPerimeters: Boolean = false,
    val overhangSpeed10MmS: Float = 0f,
    val overhangSpeed25MmS: Float = 50f,
    val overhangSpeed50MmS: Float = 30f,
    val overhangSpeed75MmS: Float = 10f,
    val bridgeSpeedMmS: Float = 50f,
    val internalBridgeSpeedPercent: Int = 150,
    val travelSpeedMmS: Float = 350f,
    // Cura Travel & Combing Mode settings
    val combingMode: CombingMode = CombingMode.NOT_IN_SKIN,
    val maxCombDistanceWithNoRetractMm: Float = 30.0f,
    val retractBeforeOuterWall: Boolean = true,
    val avoidPrintedPartsWhenTraveling: Boolean = true,
    val avoidSupportsWhenTraveling: Boolean = false,
    val travelAvoidDistanceMm: Float = 0.625f,
    val layerStartXMm: Float = 0.0f,
    val layerStartYMm: Float = 0.0f,
    val zHopWhenRetracted: Boolean = false,
    val zHopOnlyOverPrintedParts: Boolean = false,
    val zHopHeightMm: Float = 0.20f,
    val defaultAccelerationMmS2: Float = 10000f,
    val outerWallAccelerationMmS2: Float = 5000f,
    val innerWallAccelerationMmS2: Float = 8000f,
    val bridgeAccelerationPercent: Int = 50,
    val sparseInfillAccelerationPercent: Int = 100,
    val internalSolidInfillAccelerationPercent: Int = 100,
    val firstLayerAccelerationMmS2: Float = 500f,
    val firstLayerTravelAccelerationPercent: Int = 100,
    val topSurfaceAccelerationMmS2: Float = 2000f,
    val travelAccelerationMmS2: Float = 10000f,
    val jerkDefaultMmS: Float = 9f,
    val jerkOuterWallMmS: Float = 9f,
    val jerkInnerWallMmS: Float = 9f,
    val jerkInfillMmS: Float = 9f,
    val jerkTopSurfaceMmS: Float = 9f,
    val jerkFirstLayerMmS: Float = 9f,
    val jerkFirstLayerTravelMmS: Float = 9f,
    val jerkTravelMmS: Float = 12f,
    val extrusionRateSmoothingMm3S2: Float = 0f,

    // 4. Support
    val enableSupport: Boolean = false,
    val supportType: SupportType = SupportType.NORMAL_GRID,
    val supportStyle: SupportStyle = SupportStyle.DEFAULT,
    val supportThresholdAngleDeg: Int = 30,
    val supportThresholdOverlapPercent: Int = 50,
    val supportFirstLayerDensityPercent: Int = 90,
    val supportFirstLayerExpansionMm: Float = 2.0f,
    val supportOnBuildPlateOnly: Boolean = false,
    val removeSmallOverhangs: Boolean = true,
    val raftLayers: Int = 0,
    val supportFilamentBase: String = "Default",
    val supportFilamentInterface: String = "Default",
    val ironingSupportInterface: Boolean = false,
    val supportTopZDistanceMm: Float = 0.20f,
    val supportBottomZDistanceMm: Float = 0.20f,
    val supportWallLoops: Int = 0,
    val supportBasePattern: SupportBasePattern = SupportBasePattern.RECTILINEAR,
    val supportBasePatternSpacingMm: Float = 2.5f,
    val supportPatternAngleDeg: Int = 0,
    val supportTopInterfaceLayers: Int = 2,
    val supportBottomInterfaceLayers: Int = 2,
    val supportInterfacePattern: SupportInterfacePattern = SupportInterfacePattern.DEFAULT,
    val supportTopInterfaceSpacingMm: Float = 0.5f,
    val supportBottomInterfaceSpacingMm: Float = 0.5f,
    val normalSupportExpansionMm: Float = 0.0f,
    val supportObjectXyDistanceMm: Float = 0.35f,
    val supportObjectFirstLayerGapMm: Float = 0.20f,
    val dontSupportBridges: Boolean = true,
    val treeSupportTipDiameterMm: Float = 0.8f,
    val treeSupportBranchDistanceMm: Float = 1.0f,
    val treeSupportBranchDiameterMm: Float = 2.0f,
    val treeSupportBranchAngleDeg: Int = 45,
    val treeSupportBranchDiameterAngleDeg: Int = 5,
    val treeSupportWallLoops: Int = 2,
    val treeSupportBrimWidthMm: Float = 3.0f,

    // 5. Multimaterial (grouped to stay well below the JVM 255-parameter constructor slot limit)
    val multimaterial: MultimaterialSettings = MultimaterialSettings(),

    // 6. Others
    val skirtLoops: Int = 0,
    val skirtType: SkirtType = SkirtType.COMBINED,
    val skirtMinExtrusionLengthMm: Float = 0f,
    val skirtDistanceMm: Float = 2.0f,
    val skirtStartPoint: String = "Default",
    val skirtHeightLayers: Int = 1,
    val skirtSpeedMmS: Float = 50f,
    val draftShieldMode: DraftShieldMode = DraftShieldMode.DISABLED,
    val brimType: BrimType = BrimType.NONE,
    val brimWidthMm: Float = 5.0f,
    val brimObjectGapMm: Float = 0.1f,
    val brimFlowRatio: Float = 1.0f,
    val brimFollowsCompensatedOutline: Boolean = false,
    val combineBrims: Boolean = false,
    val slicingMode: SlicingModeType = SlicingModeType.REGULAR,
    val printSequence: PrintSequence = PrintSequence.BY_LAYER,
    val intraLayerOrder: IntraLayerOrder = IntraLayerOrder.DEFAULT,
    val spiralVaseMode: Boolean = false,
    val fuzzySkinType: FuzzySkinType = FuzzySkinType.NONE,
    val fuzzySkinPointDistMm: Float = 0.8f,
    val fuzzySkinThicknessMm: Float = 0.3f,
    val reduceInfillRetraction: Boolean = true,
    val verboseGcode: Boolean = false,
    val labelObjects: Boolean = true,
    val excludeObjectGcode: Boolean = true,
    val filenameFormat: String = "{input_filename_base}_{filament_type[initial_tool]}_{print_time}.gcode",
    val changeExtrusionRoleGcode: String = "",
    val postProcessingScripts: String = "",
    val notes: String = ""
)

data class MultimaterialSettings(
    val enablePrimeTower: Boolean = false,
    val primeTowerSkipPoints: Boolean = false,
    val primeTowerInterfaceFeatures: Boolean = false,
    val primeTowerInternalRibs: Boolean = true,
    val primeTowerWidthMm: Float = 60f,
    val primeTowerBrimWidthMm: Float = 3.0f,
    val primeTowerInfillGapPercent: Int = 150,
    val primeTowerRotationAngleDeg: Int = 0,
    val primeTowerMaxBridgingDistMm: Float = 10f,
    val primeTowerPurgeLinesSpacingPercent: Int = 100,
    val primeTowerExtraFlowPurgingPercent: Int = 100,
    val primeTowerMaxPrintSpeedMmS: Float = 90f,
    val primeTowerWallType: WipeTowerWallType = WipeTowerWallType.RIB,
    val primeTowerExtraRibLengthMm: Float = 0f,
    val primeTowerRibWidthMm: Float = 8f,
    val primeTowerFilletWall: Boolean = true,
    val primeTowerNoSparseLayers: Boolean = false,
    val primeVolumeMm3: Float = 45f,
    val filamentOuterWalls: String = "Default",
    val filamentInnerWalls: String = "Default",
    val filamentInfill: String = "Default",
    val filamentInternalSolidInfill: String = "Default",
    val filamentTopSurface: String = "Default",
    val filamentBottomSurface: String = "Default",
    val filamentWipeTower: String = "Default",
    val enableOozeShield: Boolean = false,
    val flushIntoInfill: Boolean = false,
    val flushIntoSupport: Boolean = true,
    val useBeamInterlocking: Boolean = false,
    val interfaceShells: Boolean = false,
    val maxWidthSegmentedRegionMm: Float = 0f,
    val interlockingDepthSegmentedRegionMm: Float = 0f
)

enum class ToolpathType(
    val gcodeComment: String,
    val displayName: String,
    val r: Float,
    val g: Float,
    val b: Float
) {
    OUTER_WALL("Outer wall", "Outer Wall", 1.00f, 0.56f, 0.20f),
    INNER_WALL("Inner wall", "Inner Wall", 0.98f, 0.82f, 0.22f),
    TOP_SURFACE("Top surface", "Top Surface", 0.96f, 0.30f, 0.22f),
    BOTTOM_SURFACE("Bottom surface", "Bottom Surface", 0.18f, 0.78f, 0.94f),
    SOLID_INFILL("Internal solid infill", "Internal Solid Infill", 0.68f, 0.38f, 0.92f),
    SPARSE_INFILL("Sparse infill", "Sparse Infill", 0.78f, 0.26f, 0.32f),
    SUPPORT("Support", "Support", 0.20f, 0.82f, 0.48f),
    SKIRT_BRIM("Skirt/Brim", "Skirt / Brim", 0.18f, 0.75f, 0.65f),
    TRAVEL("Travel", "Travel Move", 0.25f, 0.45f, 0.62f)
}

data class ToolpathSegment(
    val start: Vec3,
    val end: Vec3,
    val type: ToolpathType,
    val extrusionMm: Float,
    val speedMmS: Float,
    val layerIndex: Int,
    val widthMm: Float = 0.42f,
    val isCombed: Boolean = false,
    val requiresRetract: Boolean = false
)

data class SliceLayer(
    val index: Int,
    val zHeightMm: Float,
    val layerHeightMm: Float,
    val segments: List<ToolpathSegment>
)

data class PrintStatistics(
    val layerCount: Int,
    val totalSegmentCount: Int,
    val filamentLengthMeters: Float,
    val filamentWeightGrams: Float,
    val estimatedCostUsd: Float,
    val totalTimeSeconds: Float,
    val featureTimeSeconds: Map<ToolpathType, Float>,
    val filamentVolumeCm3: Float = 0f,
    val modelWeightGrams: Float = filamentWeightGrams,
    val supportWasteWeightGrams: Float = 0f
) {
    val formattedTotalTime: String
        get() {
            val totalSec = totalTimeSeconds.toInt().coerceAtLeast(1)
            val hrs = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            return if (hrs > 0) "${hrs}h ${mins}m ${secs}s" else "${mins}m ${secs}s"
        }

    fun calculateFullCostBreakdown(
        printer: PrinterProfile,
        filament: FilamentProfile
    ): CostBreakdown {
        val materialCost = (filamentWeightGrams / 1000f) * filament.costPerKgUsd
        val hours = totalTimeSeconds / 3600f
        val kwhUsed = (printer.powerConsumptionWatts / 1000f) * hours
        val electricityCost = kwhUsed * printer.electricityRatePerKwh
        val machineWearCost = hours * printer.hourlyMachineRate
        val totalProduction = materialCost + electricityCost + machineWearCost
        val quotePrice = totalProduction * filament.markupMultiplier
        val spoolPct = ((filamentWeightGrams / filament.spoolWeightGrams.coerceAtLeast(1f)) * 100f).coerceIn(0f, 999f)
        val printsPerSpool = if (filamentWeightGrams > 0.05f) {
            (filament.spoolWeightGrams / filamentWeightGrams).toInt()
        } else 0

        return CostBreakdown(
            currencySymbol = filament.currencySymbol,
            materialCost = materialCost,
            electricityCost = electricityCost,
            machineWearCost = machineWearCost,
            totalProductionCost = totalProduction,
            suggestedQuotePrice = quotePrice,
            kwhUsed = kwhUsed,
            spoolUsagePercent = spoolPct,
            printsPerSpool = printsPerSpool
        )
    }
}

data class CostBreakdown(
    val currencySymbol: String,
    val materialCost: Float,
    val electricityCost: Float,
    val machineWearCost: Float,
    val totalProductionCost: Float,
    val suggestedQuotePrice: Float,
    val kwhUsed: Float,
    val spoolUsagePercent: Float,
    val printsPerSpool: Int
)

class SliceResult(
    val layers: List<SliceLayer>,
    val statistics: PrintStatistics,
    val gcodeText: String
) {
    val gpuToolpathVertices: FloatArray
    val layerStartVertexIndex: IntArray
    val layerVertexCount: IntArray

    // Volumetric 3D ribbon triangles (6 vertices per extruded segment) so top/bottom surfaces render solid on mobile GPUs
    val gpuRibbonVertices: FloatArray
    val layerStartRibbonVertexIndex: IntArray
    val layerRibbonVertexCount: IntArray

    init {
        var rawTotalSegments = 0
        var rawTotalExtrusionSegments = 0
        for (l in layers) {
            rawTotalSegments += l.segments.size
            for (s in l.segments) {
                if (s.type != ToolpathType.TRAVEL) rawTotalExtrusionSegments++
            }
        }

        // Cap GPU VBO vertex buffers on ultra-complex prints so mobile heaps never hit OutOfMemoryError
        val maxGpuSegments = 120_000
        val segStride = if (rawTotalSegments > maxGpuSegments) {
            ((rawTotalSegments + maxGpuSegments - 1) / maxGpuSegments).coerceAtLeast(1)
        } else {
            1
        }

        var sampledSegments = 0
        var sampledExtrusionSegments = 0
        for (l in layers) {
            val segs = l.segments
            var i = 0
            while (i < segs.size) {
                sampledSegments++
                if (segs[i].type != ToolpathType.TRAVEL) sampledExtrusionSegments++
                i += segStride
            }
        }

        val verts = FloatArray(sampledSegments * 14)
        val starts = IntArray(layers.size)
        val counts = IntArray(layers.size)

        val ribbonVerts = FloatArray(sampledExtrusionSegments * 6 * 7)
        val ribbonStarts = IntArray(layers.size)
        val ribbonCounts = IntArray(layers.size)

        var floatIdx = 0
        var vertCursor = 0
        var ribbonFloatIdx = 0
        var ribbonVertCursor = 0

        for ((layerIdx, layer) in layers.withIndex()) {
            starts[layerIdx] = vertCursor
            var layerLineVerts = 0

            ribbonStarts[layerIdx] = ribbonVertCursor
            var layerRibbonVerts = 0

            val segs = layer.segments
            var sIdx = 0
            while (sIdx < segs.size) {
                val seg = segs[sIdx]
                sIdx += segStride

                val t = seg.type
                val tf = t.ordinal.toFloat()
                verts[floatIdx++] = seg.start.x
                verts[floatIdx++] = seg.start.y
                verts[floatIdx++] = seg.start.z
                verts[floatIdx++] = t.r
                verts[floatIdx++] = t.g
                verts[floatIdx++] = t.b
                verts[floatIdx++] = tf

                verts[floatIdx++] = seg.end.x
                verts[floatIdx++] = seg.end.y
                verts[floatIdx++] = seg.end.z
                verts[floatIdx++] = t.r
                verts[floatIdx++] = t.g
                verts[floatIdx++] = t.b
                verts[floatIdx++] = tf

                layerLineVerts += 2
                vertCursor += 2

                if (t != ToolpathType.TRAVEL) {
                    val dx = seg.end.x - seg.start.x
                    val dy = seg.end.y - seg.start.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    val halfW = (seg.widthMm.coerceIn(0.20f, 1.20f)) * 0.50f
                    val nx = if (len > 1e-5f) (-dy / len) * halfW else 0f
                    val ny = if (len > 1e-5f) (dx / len) * halfW else halfW
                    val z = seg.start.z

                    // 4 corners of the extruded bead ribbon
                    val x0L = seg.start.x + nx; val y0L = seg.start.y + ny
                    val x0R = seg.start.x - nx; val y0R = seg.start.y - ny
                    val x1L = seg.end.x + nx;   val y1L = seg.end.y + ny
                    val x1R = seg.end.x - nx;   val y1R = seg.end.y - ny

                    // Slight edge shading (0.90x on left/right edges, 1.04x on top-right) for 3D bead relief
                    val rL = (t.r * 1.03f).coerceAtMost(1f)
                    val gL = (t.g * 1.03f).coerceAtMost(1f)
                    val bL = (t.b * 1.03f).coerceAtMost(1f)
                    val rR = t.r * 0.86f
                    val gR = t.g * 0.86f
                    val bR = t.b * 0.86f

                    // Triangle 1: (0R, 0L, 1L)
                    ribbonVerts[ribbonFloatIdx++] = x0R; ribbonVerts[ribbonFloatIdx++] = y0R; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rR;  ribbonVerts[ribbonFloatIdx++] = gR;  ribbonVerts[ribbonFloatIdx++] = bR; ribbonVerts[ribbonFloatIdx++] = tf

                    ribbonVerts[ribbonFloatIdx++] = x0L; ribbonVerts[ribbonFloatIdx++] = y0L; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rL;  ribbonVerts[ribbonFloatIdx++] = gL;  ribbonVerts[ribbonFloatIdx++] = bL; ribbonVerts[ribbonFloatIdx++] = tf

                    ribbonVerts[ribbonFloatIdx++] = x1L; ribbonVerts[ribbonFloatIdx++] = y1L; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rL;  ribbonVerts[ribbonFloatIdx++] = gL;  ribbonVerts[ribbonFloatIdx++] = bL; ribbonVerts[ribbonFloatIdx++] = tf

                    // Triangle 2: (0R, 1L, 1R)
                    ribbonVerts[ribbonFloatIdx++] = x0R; ribbonVerts[ribbonFloatIdx++] = y0R; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rR;  ribbonVerts[ribbonFloatIdx++] = gR;  ribbonVerts[ribbonFloatIdx++] = bR; ribbonVerts[ribbonFloatIdx++] = tf

                    ribbonVerts[ribbonFloatIdx++] = x1L; ribbonVerts[ribbonFloatIdx++] = y1L; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rL;  ribbonVerts[ribbonFloatIdx++] = gL;  ribbonVerts[ribbonFloatIdx++] = bL; ribbonVerts[ribbonFloatIdx++] = tf

                    ribbonVerts[ribbonFloatIdx++] = x1R; ribbonVerts[ribbonFloatIdx++] = y1R; ribbonVerts[ribbonFloatIdx++] = z
                    ribbonVerts[ribbonFloatIdx++] = rR;  ribbonVerts[ribbonFloatIdx++] = gR;  ribbonVerts[ribbonFloatIdx++] = bR; ribbonVerts[ribbonFloatIdx++] = tf

                    layerRibbonVerts += 6
                    ribbonVertCursor += 6
                }
            }
            counts[layerIdx] = layerLineVerts
            ribbonCounts[layerIdx] = layerRibbonVerts
        }
        gpuToolpathVertices = verts
        layerStartVertexIndex = starts
        layerVertexCount = counts

        gpuRibbonVertices = ribbonVerts
        layerStartRibbonVertexIndex = ribbonStarts
        layerRibbonVertexCount = ribbonCounts
    }
}

/**
 * Complete Built-In OrcaSlicer Catalog of 55+ Printer Profiles, 48+ Filament Profiles, and 10 Process Presets.
 */
object BuiltInPresets {

    private fun p(
        id: String,
        vendor: String,
        name: String,
        x: Float,
        y: Float,
        z: Float,
        nozzle: Float = 0.4f,
        flavor: GCodeFlavor = GCodeFlavor.KLIPPER,
        speed: Float = 500f,
        accel: Float = 12000f,
        watts: Float = 180f
    ) = PrinterProfile(
        id = id,
        vendor = vendor,
        name = name,
        bedWidthX = x,
        bedDepthY = y,
        maxPrintHeightZ = z,
        nozzleDiameterMm = nozzle,
        gcodeFlavor = flavor,
        maxSpeedXYMmS = speed,
        maxAccelerationMmS2 = accel,
        powerConsumptionWatts = watts
    )

    val printers: List<PrinterProfile> = listOf(
        // Bambu Lab
        p("bambu_x1c_04", "Bambu Lab", "Bambu Lab X1 Carbon 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.BAMBU, 500f, 20000f, 220f),
        p("bambu_x1c_02", "Bambu Lab", "Bambu Lab X1 Carbon 0.2 nozzle", 256f, 256f, 256f, 0.2f, GCodeFlavor.BAMBU, 350f, 15000f, 220f),
        p("bambu_x1c_06", "Bambu Lab", "Bambu Lab X1 Carbon 0.6 nozzle", 256f, 256f, 256f, 0.6f, GCodeFlavor.BAMBU, 500f, 20000f, 230f),
        p("bambu_x1c_08", "Bambu Lab", "Bambu Lab X1 Carbon 0.8 nozzle", 256f, 256f, 256f, 0.8f, GCodeFlavor.BAMBU, 450f, 18000f, 240f),
        p("bambu_x1e_04", "Bambu Lab", "Bambu Lab X1E 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.BAMBU, 500f, 20000f, 280f),
        p("bambu_p1s_04", "Bambu Lab", "Bambu Lab P1S 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.BAMBU, 500f, 20000f, 200f),
        p("bambu_p1p_04", "Bambu Lab", "Bambu Lab P1P 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.BAMBU, 500f, 20000f, 190f),
        p("bambu_a1_04", "Bambu Lab", "Bambu Lab A1 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.BAMBU, 500f, 10000f, 170f),
        p("bambu_a1_mini_04", "Bambu Lab", "Bambu Lab A1 Mini 0.4 nozzle", 180f, 180f, 180f, 0.4f, GCodeFlavor.BAMBU, 500f, 10000f, 120f),

        // Creality
        p("creality_ender5_plus", "Creality", "Creality Ender-5 Plus 0.4 nozzle", 350f, 350f, 400f, 0.4f, GCodeFlavor.KLIPPER, 350f, 5000f, 300f),
        p("creality_k2_plus", "Creality", "Creality K2 Plus 0.4 nozzle", 350f, 350f, 350f, 0.4f, GCodeFlavor.KLIPPER, 600f, 30000f, 350f),
        p("creality_k1_max", "Creality", "Creality K1 Max 0.4 nozzle", 300f, 300f, 300f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 280f),
        p("creality_k1c", "Creality", "Creality K1C Carbon 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 220f),
        p("creality_k1", "Creality", "Creality K1 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 220f),
        p("creality_ender3_v3", "Creality", "Creality Ender-3 V3 CoreXZ 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.KLIPPER, 500f, 20000f, 180f),
        p("creality_ender3_v3_ke", "Creality", "Creality Ender-3 V3 KE 0.4 nozzle", 220f, 220f, 240f, 0.4f, GCodeFlavor.KLIPPER, 500f, 8000f, 170f),
        p("creality_ender3_v3_se", "Creality", "Creality Ender-3 V3 SE 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.MARLIN, 250f, 4000f, 150f),
        p("creality_ender3_s1_pro", "Creality", "Creality Ender-3 S1 Pro 0.4 nozzle", 220f, 220f, 270f, 0.4f, GCodeFlavor.MARLIN, 180f, 2500f, 150f),
        p("creality_ender3_v2", "Creality", "Creality Ender-3 V2 / Neo 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.MARLIN, 150f, 1500f, 140f),
        p("creality_ender5_s1", "Creality", "Creality Ender-5 S1 0.4 nozzle", 220f, 220f, 280f, 0.4f, GCodeFlavor.MARLIN, 250f, 4000f, 180f),
        p("creality_cr10_se", "Creality", "Creality CR-10 SE 0.4 nozzle", 220f, 220f, 265f, 0.4f, GCodeFlavor.KLIPPER, 600f, 12000f, 200f),

        // Prusa Research
        p("prusa_core_one", "Prusa", "Prusa CORE One 0.4 nozzle", 250f, 220f, 270f, 0.4f, GCodeFlavor.PRUSA, 400f, 10000f, 180f),
        p("prusa_mk4s", "Prusa", "Original Prusa MK4 / MK4S 0.4 nozzle", 250f, 210f, 220f, 0.4f, GCodeFlavor.PRUSA, 350f, 6000f, 150f),
        p("prusa_xl_360", "Prusa", "Original Prusa XL 360mm 0.4 nozzle", 360f, 360f, 360f, 0.4f, GCodeFlavor.PRUSA, 400f, 7000f, 300f),
        p("prusa_mk3s", "Prusa", "Original Prusa i3 MK3S+ 0.4 nozzle", 250f, 210f, 210f, 0.4f, GCodeFlavor.PRUSA, 200f, 2500f, 130f),
        p("prusa_mini", "Prusa", "Original Prusa MINI+ Input Shaper 0.4 nozzle", 180f, 180f, 180f, 0.4f, GCodeFlavor.PRUSA, 250f, 4000f, 100f),

        // Voron Design
        p("voron_24_350", "Voron", "Voron 2.4 350mm 0.4 nozzle", 350f, 350f, 330f, 0.4f, GCodeFlavor.KLIPPER, 500f, 15000f, 320f),
        p("voron_24_300", "Voron", "Voron 2.4 300mm 0.4 nozzle", 300f, 300f, 280f, 0.4f, GCodeFlavor.KLIPPER, 500f, 15000f, 260f),
        p("voron_24_250", "Voron", "Voron 2.4 250mm 0.4 nozzle", 250f, 250f, 230f, 0.4f, GCodeFlavor.KLIPPER, 500f, 18000f, 220f),
        p("voron_trident_350", "Voron", "Voron Trident 350mm 0.4 nozzle", 350f, 350f, 250f, 0.4f, GCodeFlavor.KLIPPER, 500f, 15000f, 300f),
        p("voron_trident_300", "Voron", "Voron Trident 300mm 0.4 nozzle", 300f, 300f, 250f, 0.4f, GCodeFlavor.KLIPPER, 500f, 15000f, 250f),
        p("voron_v02_120", "Voron", "Voron V0.2 120mm 0.4 nozzle", 120f, 120f, 120f, 0.4f, GCodeFlavor.KLIPPER, 500f, 20000f, 110f),
        p("voron_switchwire", "Voron", "Voron Switchwire 250mm 0.4 nozzle", 250f, 210f, 240f, 0.4f, GCodeFlavor.KLIPPER, 350f, 8000f, 180f),

        // Qidi Tech
        p("qidi_plus4", "Qidi", "Qidi Plus 4 305mm 0.4 nozzle", 305f, 305f, 280f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 320f),
        p("qidi_q1_pro", "Qidi", "Qidi Q1 Pro 245mm 0.4 nozzle", 245f, 245f, 240f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 260f),
        p("qidi_xmax3", "Qidi", "Qidi X-Max 3 325mm 0.4 nozzle", 325f, 325f, 315f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 340f),
        p("qidi_xplus3", "Qidi", "Qidi X-Plus 3 280mm 0.4 nozzle", 280f, 280f, 270f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 280f),
        p("qidi_xsmart3", "Qidi", "Qidi X-Smart 3 175mm 0.4 nozzle", 175f, 180f, 170f, 0.4f, GCodeFlavor.KLIPPER, 500f, 20000f, 180f),

        // Sovol
        p("sovol_sv08", "Sovol", "Sovol SV08 CoreXY 350mm 0.4 nozzle", 350f, 350f, 345f, 0.4f, GCodeFlavor.KLIPPER, 700f, 25000f, 320f),
        p("sovol_sv07_plus", "Sovol", "Sovol SV07 Plus 300mm 0.4 nozzle", 300f, 300f, 350f, 0.4f, GCodeFlavor.KLIPPER, 500f, 8000f, 240f),
        p("sovol_sv07", "Sovol", "Sovol SV07 220mm 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.KLIPPER, 500f, 8000f, 180f),
        p("sovol_sv06_plus", "Sovol", "Sovol SV06 Plus 300mm 0.4 nozzle", 300f, 300f, 340f, 0.4f, GCodeFlavor.MARLIN, 250f, 4000f, 220f),
        p("sovol_sv06", "Sovol", "Sovol SV06 220mm 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.MARLIN, 250f, 4000f, 160f),

        // Elegoo
        p("elegoo_neptune4_max", "Elegoo", "Elegoo Neptune 4 Max 420mm 0.4 nozzle", 420f, 420f, 480f, 0.4f, GCodeFlavor.KLIPPER, 500f, 8000f, 360f),
        p("elegoo_neptune4_plus", "Elegoo", "Elegoo Neptune 4 Plus 320mm 0.4 nozzle", 320f, 320f, 385f, 0.4f, GCodeFlavor.KLIPPER, 500f, 10000f, 280f),
        p("elegoo_neptune4_pro", "Elegoo", "Elegoo Neptune 4 / 4 Pro 225mm 0.4 nozzle", 225f, 225f, 265f, 0.4f, GCodeFlavor.KLIPPER, 500f, 12000f, 190f),

        // Anycubic
        p("anycubic_kobra3", "Anycubic", "Anycubic Kobra 3 Combo 250mm 0.4 nozzle", 250f, 250f, 260f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 220f),
        p("anycubic_kobra2_max", "Anycubic", "Anycubic Kobra 2 Max 420mm 0.4 nozzle", 420f, 420f, 500f, 0.4f, GCodeFlavor.KLIPPER, 500f, 10000f, 360f),
        p("anycubic_kobra2_pro", "Anycubic", "Anycubic Kobra 2 Pro 220mm 0.4 nozzle", 220f, 220f, 250f, 0.4f, GCodeFlavor.KLIPPER, 500f, 12000f, 190f),

        // FlashForge, RatRig, FLSUN, Artillery
        p("flashforge_adv5m_pro", "FlashForge", "FlashForge Adventurer 5M / 5M Pro 0.4 nozzle", 220f, 220f, 220f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 210f),
        p("ratrig_vcore4_400", "RatRig", "RatRig V-Core 4 400mm 0.4 nozzle", 400f, 400f, 400f, 0.4f, GCodeFlavor.KLIPPER, 700f, 25000f, 380f),
        p("ratrig_vcore4_300", "RatRig", "RatRig V-Core 4 300mm 0.4 nozzle", 300f, 300f, 300f, 0.4f, GCodeFlavor.KLIPPER, 700f, 25000f, 300f),
        p("flsun_s1", "FLSUN", "FLSUN S1 / T1 Delta 320mm 0.4 nozzle", 320f, 320f, 430f, 0.4f, GCodeFlavor.KLIPPER, 800f, 30000f, 350f),
        p("flsun_v400", "FLSUN", "FLSUN V400 Delta 300mm 0.4 nozzle", 300f, 300f, 410f, 0.4f, GCodeFlavor.KLIPPER, 400f, 10000f, 260f),
        p("artillery_sw_x4_plus", "Artillery", "Artillery Sidewinder X4 Plus 300mm 0.4 nozzle", 300f, 300f, 400f, 0.4f, GCodeFlavor.KLIPPER, 500f, 10000f, 260f),
        p("kingroon_klp1", "Kingroon", "Kingroon KLP1 CoreXY 230mm 0.4 nozzle", 230f, 230f, 210f, 0.4f, GCodeFlavor.KLIPPER, 500f, 15000f, 180f),
        p("twotrees_sk1", "TwoTrees", "TwoTrees SK1 CoreXY 256mm 0.4 nozzle", 256f, 256f, 256f, 0.4f, GCodeFlavor.KLIPPER, 600f, 20000f, 220f)
    )

    private fun f(
        id: String,
        vendor: String,
        name: String,
        material: String,
        color: Long,
        density: Float,
        priceKg: Float,
        nozzleInit: Int,
        nozzleOther: Int,
        bedTemp: Int,
        flow: Float = 0.98f,
        mvs: Float = 20f,
        pa: Float = 0.032f,
        fan: Int = 100,
        chamber: Int = 0
    ) = FilamentProfile(
        id = id,
        vendor = vendor,
        name = name,
        materialType = material,
        colorHex = color,
        densityGPerCm3 = density,
        costPerKgUsd = priceKg,
        nozzleTempInitialC = nozzleInit,
        nozzleTempOtherC = nozzleOther,
        bedTempInitialC = bedTemp,
        bedTempOtherC = bedTemp,
        chamberTempC = chamber,
        flowRatio = flow,
        maxVolumetricSpeedMm3S = mvs,
        pressureAdvance = pa,
        fanSpeedPercent = fan
    )

    val filaments: List<FilamentProfile> = listOf(
        // Bambu Lab Official Profiles
        f("bambu_pla_basic", "Bambu Lab", "Bambu PLA Basic", "PLA", 0xFF29ABE2, 1.24f, 19.99f, 220, 220, 55, 0.98f, 21f, 0.028f, 100),
        f("bambu_pla_matte", "Bambu Lab", "Bambu PLA Matte", "PLA-Matte", 0xFF78909C, 1.31f, 19.99f, 220, 220, 55, 0.98f, 18f, 0.032f, 100),
        f("bambu_pla_tough", "Bambu Lab", "Bambu PLA Tough", "PLA+", 0xFF2898FF, 1.23f, 24.99f, 220, 220, 55, 0.98f, 19f, 0.030f, 100),
        f("bambu_pla_cf", "Bambu Lab", "Bambu PLA-CF (Carbon Fiber)", "PLA-CF", 0xFF455A64, 1.22f, 31.99f, 225, 225, 55, 0.95f, 15f, 0.025f, 100),
        f("bambu_pla_silk", "Bambu Lab", "Bambu PLA Silk", "PLA-Silk", 0xFFFFB300, 1.25f, 24.99f, 230, 230, 55, 0.97f, 12f, 0.038f, 100),
        f("bambu_pla_aero", "Bambu Lab", "Bambu PLA Aero (Foaming LW-PLA)", "PLA-Aero", 0xFFECEFF1, 1.21f, 39.99f, 240, 240, 55, 0.55f, 8f, 0.045f, 80),
        f("bambu_petg_hf", "Bambu Lab", "Bambu PETG HF (High Flow)", "PETG", 0xFF00B0FF, 1.27f, 19.99f, 245, 245, 70, 0.96f, 20f, 0.040f, 60),
        f("bambu_petg_basic", "Bambu Lab", "Bambu PETG Basic", "PETG", 0xFF2979FF, 1.27f, 19.99f, 255, 255, 70, 0.95f, 13f, 0.045f, 50),
        f("bambu_petg_cf", "Bambu Lab", "Bambu PETG-CF", "PETG-CF", 0xFF546E7A, 1.25f, 34.99f, 255, 255, 70, 0.95f, 14f, 0.035f, 40),
        f("bambu_abs", "Bambu Lab", "Bambu ABS", "ABS", 0xFFFF6E40, 1.05f, 19.99f, 270, 270, 95, 0.95f, 18f, 0.028f, 30, 45),
        f("bambu_asa", "Bambu Lab", "Bambu ASA", "ASA", 0xFFFF5252, 1.05f, 26.99f, 265, 265, 95, 0.95f, 16f, 0.028f, 25, 45),
        f("bambu_pc", "Bambu Lab", "Bambu PC (Polycarbonate)", "PC", 0xFFB0BEC5, 1.20f, 36.99f, 280, 280, 100, 0.94f, 14f, 0.026f, 20, 50),
        f("bambu_tpu95a", "Bambu Lab", "Bambu TPU 95A HF", "TPU", 0xFFE040FB, 1.22f, 34.99f, 230, 230, 45, 1.00f, 7f, 0.070f, 80),
        f("bambu_paht_cf", "Bambu Lab", "Bambu PAHT-CF (Nylon Carbon Fiber)", "PA-CF", 0xFF37474F, 1.23f, 59.99f, 290, 290, 100, 0.96f, 12f, 0.026f, 20, 50),
        f("bambu_pa6_cf", "Bambu Lab", "Bambu PA6-CF", "PA6-CF", 0xFF263238, 1.22f, 64.99f, 285, 285, 100, 0.96f, 12f, 0.025f, 15, 50),
        f("bambu_pet_cf", "Bambu Lab", "Bambu PET-CF", "PET-CF", 0xFF455A64, 1.29f, 54.99f, 280, 280, 90, 0.95f, 12f, 0.026f, 20, 45),
        f("bambu_pva", "Bambu Lab", "Bambu PVA Water-Soluble Support", "PVA", 0xFFFFF59D, 1.23f, 44.99f, 210, 210, 55, 0.98f, 6f, 0.045f, 80),

        // eSUN Filaments
        f("esun_pla_plus", "eSUN", "eSUN PLA+ (PLA Plus)", "PLA+", 0xFF00E676, 1.24f, 17.99f, 218, 215, 60, 0.98f, 20f, 0.032f, 100),
        f("esun_pla_hs", "eSUN", "eSUN ePLA-HS High Speed", "PLA", 0xFF1DE9B6, 1.24f, 18.99f, 225, 220, 60, 0.98f, 24f, 0.028f, 100),
        f("esun_pla_matte", "eSUN", "eSUN ePLA-Matte", "PLA-Matte", 0xFF90A4AE, 1.28f, 17.99f, 215, 210, 60, 0.97f, 16f, 0.035f, 100),
        f("esun_pla_silk", "eSUN", "eSUN ePLA-Silk", "PLA-Silk", 0xFFFFD54F, 1.25f, 21.99f, 225, 220, 60, 0.97f, 12f, 0.040f, 100),
        f("esun_petg", "eSUN", "eSUN PETG", "PETG", 0xFF448AFF, 1.27f, 16.99f, 245, 240, 75, 0.96f, 14f, 0.046f, 50),
        f("esun_abs_plus", "eSUN", "eSUN ABS+", "ABS", 0xFFFF7043, 1.06f, 18.99f, 255, 250, 95, 0.95f, 16f, 0.030f, 35, 45),
        f("esun_asa", "eSUN", "eSUN eASA", "ASA", 0xFFEF5350, 1.07f, 22.99f, 260, 255, 95, 0.95f, 16f, 0.030f, 25, 45),
        f("esun_tpu95a", "eSUN", "eSUN eTPU-95A", "TPU", 0xFFAB47BC, 1.21f, 24.99f, 225, 220, 50, 1.00f, 5f, 0.075f, 80),
        f("esun_pa_cf", "eSUN", "eSUN ePA-CF Nylon Carbon", "PA-CF", 0xFF546E7A, 1.24f, 39.99f, 280, 275, 90, 0.96f, 11f, 0.028f, 20, 45),

        // Polymaker Filaments
        f("poly_terra_pla", "Polymaker", "Polymaker PolyTerra PLA", "PLA-Matte", 0xFF66BB6A, 1.31f, 19.99f, 215, 210, 55, 0.98f, 18f, 0.034f, 100),
        f("poly_lite_pla_pro", "Polymaker", "Polymaker PolyLite PLA Pro", "PLA+", 0xFF26A69A, 1.23f, 24.99f, 220, 215, 60, 0.98f, 20f, 0.030f, 100),
        f("poly_lite_petg", "Polymaker", "Polymaker PolyLite PETG", "PETG", 0xFF29B6F6, 1.27f, 21.99f, 240, 235, 70, 0.96f, 14f, 0.044f, 50),
        f("poly_lite_abs", "Polymaker", "Polymaker PolyLite ABS", "ABS", 0xFFFF8A65, 1.12f, 21.99f, 255, 250, 95, 0.95f, 16f, 0.030f, 30, 45),
        f("poly_lite_asa", "Polymaker", "Polymaker PolyLite ASA", "ASA", 0xFFE57373, 1.12f, 29.99f, 260, 255, 95, 0.95f, 16f, 0.030f, 25, 45),
        f("poly_max_pc", "Polymaker", "Polymaker PolyMax PC", "PC", 0xFFCFD8DC, 1.20f, 39.99f, 270, 265, 100, 0.94f, 13f, 0.028f, 15, 50),
        f("poly_flex_tpu95", "Polymaker", "Polymaker PolyFlex TPU95", "TPU", 0xFFCE93D8, 1.21f, 34.99f, 225, 220, 45, 1.00f, 5.5f, 0.075f, 80),
        f("poly_mide_pa6_cf", "Polymaker", "Polymaker Fiberon / PolyMide PA6-CF", "PA6-CF", 0xFF37474F, 1.22f, 59.99f, 290, 285, 50, 0.96f, 12f, 0.026f, 15, 45),

        // Prusament
        f("prusament_pla", "Prusament", "Prusament PLA", "PLA", 0xFFFF9800, 1.24f, 29.99f, 215, 215, 60, 0.98f, 18f, 0.032f, 100),
        f("prusament_petg", "Prusament", "Prusament PETG", "PETG", 0xFFFF6D00, 1.27f, 29.99f, 245, 250, 85, 0.96f, 15f, 0.042f, 50),
        f("prusament_asa", "Prusament", "Prusament ASA", "ASA", 0xFFF44336, 1.07f, 32.99f, 260, 260, 105, 0.95f, 16f, 0.030f, 20, 45),
        f("prusament_pc_blend", "Prusament", "Prusament PC Blend", "PC", 0xFF90A4AE, 1.22f, 44.99f, 275, 275, 110, 0.95f, 13f, 0.028f, 20, 50),

        // Overture & Sunlu
        f("overture_pla_pro", "Overture", "Overture PLA Professional", "PLA+", 0xFF00C896, 1.24f, 16.99f, 215, 210, 60, 0.98f, 19f, 0.034f, 100),
        f("overture_petg", "Overture", "Overture PETG", "PETG", 0xFF0288D1, 1.27f, 15.99f, 245, 240, 80, 0.96f, 14f, 0.045f, 50),
        f("sunlu_pla_plus", "Sunlu", "Sunlu PLA+ / High Speed PLA", "PLA+", 0xFF4CAF50, 1.24f, 14.99f, 220, 215, 60, 0.98f, 22f, 0.030f, 100),
        f("sunlu_petg", "Sunlu", "Sunlu PETG", "PETG", 0xFF03A9F4, 1.27f, 13.99f, 240, 235, 75, 0.96f, 14f, 0.045f, 50),

        // Generic Profiles
        f("generic_pla", "Generic", "Generic PLA", "PLA", 0xFF00C896, 1.24f, 15.99f, 215, 210, 60, 0.98f, 18f, 0.035f, 100),
        f("generic_pla_hs", "Generic", "Generic High-Speed PLA", "PLA", 0xFF00E5FF, 1.24f, 17.99f, 225, 220, 60, 0.98f, 24f, 0.028f, 100),
        f("generic_petg", "Generic", "Generic PETG", "PETG", 0xFF2898FF, 1.27f, 16.99f, 245, 240, 75, 0.96f, 14f, 0.045f, 50),
        f("generic_abs", "Generic", "Generic ABS", "ABS", 0xFFFF6E40, 1.05f, 17.99f, 260, 255, 100, 0.95f, 16f, 0.030f, 30, 45),
        f("generic_asa", "Generic", "Generic ASA", "ASA", 0xFFEF5350, 1.07f, 21.99f, 260, 255, 100, 0.95f, 16f, 0.030f, 25, 45),
        f("generic_tpu95a", "Generic", "Generic TPU 95A Flexible", "TPU", 0xFFE040FB, 1.21f, 23.99f, 230, 225, 50, 1.00f, 4.5f, 0.080f, 80),
        f("generic_pa", "Generic", "Generic PA (Nylon)", "PA", 0xFFCFD8DC, 1.15f, 34.99f, 275, 270, 90, 0.96f, 12f, 0.030f, 20, 45),
        f("generic_hips", "Generic", "Generic HIPS", "HIPS", 0xFFFFF9C4, 1.04f, 22.99f, 240, 235, 95, 0.96f, 14f, 0.035f, 40, 40)
    )

    val processes: List<ProcessProfile> = listOf(
        ProcessProfile(
            id = "020_standard",
            name = "0.20mm Standard @Orca",
            layerHeightMm = 0.20f,
            initialLayerHeightMm = 0.20f,
            wallLoops = 2,
            topShellLayers = 4,
            bottomShellLayers = 3,
            sparseInfillDensityPercent = 15,
            sparseInfillPattern = InfillPattern.CROSS_HATCH
        ),
        ProcessProfile(
            id = "008_extra_fine",
            name = "0.08mm Extra Fine @Orca",
            layerHeightMm = 0.08f,
            initialLayerHeightMm = 0.16f,
            wallLoops = 4,
            topShellLayers = 7,
            bottomShellLayers = 5,
            sparseInfillDensityPercent = 20,
            sparseInfillPattern = InfillPattern.GYROID,
            outerWallSpeedMmS = 70f,
            innerWallSpeedMmS = 130f
        ),
        ProcessProfile(
            id = "012_fine",
            name = "0.12mm Fine Detail @Orca",
            layerHeightMm = 0.12f,
            initialLayerHeightMm = 0.20f,
            wallLoops = 4,
            topShellLayers = 6,
            bottomShellLayers = 5,
            sparseInfillDensityPercent = 20,
            sparseInfillPattern = InfillPattern.GYROID,
            outerWallSpeedMmS = 80f,
            innerWallSpeedMmS = 150f
        ),
        ProcessProfile(
            id = "016_optimal",
            name = "0.16mm Optimal @Orca",
            layerHeightMm = 0.16f,
            initialLayerHeightMm = 0.20f,
            wallLoops = 3,
            topShellLayers = 5,
            bottomShellLayers = 4,
            sparseInfillDensityPercent = 18,
            sparseInfillPattern = InfillPattern.GYROID,
            outerWallSpeedMmS = 100f
        ),
        ProcessProfile(
            id = "020_strength",
            name = "0.20mm Strength 6-Wall @Orca",
            layerHeightMm = 0.20f,
            initialLayerHeightMm = 0.24f,
            wallLoops = 6,
            topShellLayers = 5,
            bottomShellLayers = 5,
            sparseInfillDensityPercent = 40,
            sparseInfillPattern = InfillPattern.HONEYCOMB
        ),
        ProcessProfile(
            id = "024_draft",
            name = "0.24mm Draft @Orca",
            layerHeightMm = 0.24f,
            initialLayerHeightMm = 0.24f,
            wallLoops = 2,
            topShellLayers = 4,
            bottomShellLayers = 3,
            sparseInfillDensityPercent = 15,
            sparseInfillPattern = InfillPattern.GRID,
            outerWallSpeedMmS = 150f,
            innerWallSpeedMmS = 220f
        ),
        ProcessProfile(
            id = "028_extra_draft",
            name = "0.28mm Extra Draft @Orca",
            layerHeightMm = 0.28f,
            initialLayerHeightMm = 0.28f,
            wallLoops = 2,
            topShellLayers = 3,
            bottomShellLayers = 3,
            sparseInfillDensityPercent = 12,
            sparseInfillPattern = InfillPattern.GRID,
            outerWallSpeedMmS = 160f,
            innerWallSpeedMmS = 240f
        ),
        ProcessProfile(
            id = "020_vase",
            name = "0.20mm Spiral Vase Mode @Orca",
            layerHeightMm = 0.20f,
            initialLayerHeightMm = 0.24f,
            wallLoops = 1,
            topShellLayers = 0,
            bottomShellLayers = 3,
            sparseInfillDensityPercent = 0,
            spiralVaseMode = true,
            outerWallSpeedMmS = 70f
        ),
        ProcessProfile(
            id = "020_fuzzy_skin",
            name = "0.20mm Textured Fuzzy Skin @Orca",
            layerHeightMm = 0.20f,
            initialLayerHeightMm = 0.24f,
            wallLoops = 3,
            sparseInfillDensityPercent = 15,
            fuzzySkinType = FuzzySkinType.CONTOUR,
            fuzzySkinPointDistMm = 0.6f,
            fuzzySkinThicknessMm = 0.3f
        ),
        ProcessProfile(
            id = "036_high_flow_06",
            name = "0.36mm High-Flow (0.6/0.8 Nozzle) @Orca",
            layerHeightMm = 0.36f,
            initialLayerHeightMm = 0.36f,
            lineWidthMm = 0.65f,
            outerWallLineWidthMm = 0.62f,
            innerWallLineWidthMm = 0.68f,
            wallLoops = 2,
            sparseInfillDensityPercent = 15,
            sparseInfillPattern = InfillPattern.GYROID
        )
    )
}
