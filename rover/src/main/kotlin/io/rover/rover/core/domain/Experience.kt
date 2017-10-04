package io.rover.rover.core.domain

import java.net.URI

/**
 * A Rover experience.
 */
data class Experience(
    val id: ID,
    val homeScreenId: ID,
    val screens: List<Screen>
) {
    companion object
}

interface Background {
    val backgroundColor: Color
    val backgroundContentMode: BackgroundContentMode
    val backgroundImage: Image?
    val backgroundScale: BackgroundScale

    companion object
}

enum class BackgroundContentMode(
    val wireFormat: String
) {
    Original("ORIGINAL"),
    Stretch("STRETCH"),
    Tile("TILE"),
    Fill("FILL"),
    Fit("FIT");

    companion object
}

enum class BackgroundScale(
    val wireFormat: String
) {
    X1("X1"),
    X2("X2"),
    X3("X3");

    companion object
}

interface Block {
    val action: BlockAction?
    val autoHeight: Boolean
    val height: Length
    val id: ID
    val insets: Insets
    val horizontalAlignment: HorizontalAlignment
    val offsets: Offsets
    val opacity: Double
    val position: Position
    val verticalAlignment: VerticalAlignment
    val width: Length

    companion object
}

data class BarcodeBlock (
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    val barcodeScale: Int,
    val barcodeText: String,
    val barcodeFormat: BarcodeFormat,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val height: Length,
    override val id: ID,
    override val insets: Insets,
    override val horizontalAlignment: HorizontalAlignment,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block, Background, Border {
    companion object
}

data class ButtonBlock(
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    val disabled: ButtonState,
    override val height: Length,
    val highlighted: ButtonState,
    override val horizontalAlignment: HorizontalAlignment,
    override val id: ID,
    override val insets: Insets,
    val normal: ButtonState,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    val selected: ButtonState,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block {
    companion object
}

data class ImageBlock(
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val height: Length,
    override val id: ID,
    val image: Image?,
    override val insets: Insets,
    override val horizontalAlignment: HorizontalAlignment,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block, Background, Border {
    companion object
}

data class RectangleBlock(
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val height: Length,
    override val id: ID,
    override val insets: Insets,
    override val horizontalAlignment: HorizontalAlignment,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block, Background, Border {
    companion object
}

data class TextBlock(
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val height: Length,
    override val id: ID,
    override val insets: Insets,
    override val horizontalAlignment: HorizontalAlignment,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    override val textAlignment: TextAlignment,
    override val textColor: Color,
    override val textFont: Font,
    override val text: String,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block, Background, Border, Text {
    companion object
}

data class WebViewBlock(
    override val action: BlockAction?,
    override val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val height: Length,
    override val id: ID,
    override val insets: Insets,
    val isScrollingEnabled: Boolean,
    override val horizontalAlignment: HorizontalAlignment,
    override val offsets: Offsets,
    override val opacity: Double,
    override val position: Position,
    val url: URI,
    override val verticalAlignment: VerticalAlignment,
    override val width: Length
): Block, Background, Border {
    companion object
}

enum class BarcodeFormat(
    val wireFormat: String
) {
    QrCode("QRCODE"),
    AztecCode("AZTECCODE"),
    Pdf417("PDF417"),
    Code128("CODE128");

    companion object
}

sealed class BlockAction {
    class OpenUrlAction(
        val url: URI
    ): BlockAction() {
        companion object
    }
    class GoToScreenAction(
        val experienceId: ID,
        val screenId: ID
    ): BlockAction() {
        companion object
    }

    companion object
}

interface Border {
    val borderColor: Color
    val borderRadius: Int
    val borderWidth: Int

    companion object
}

data class ButtonState (
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    override val borderColor: Color,
    override val borderRadius: Int,
    override val borderWidth: Int,
    override val textAlignment: TextAlignment,
    override val textColor: Color,
    override val textFont: Font,
    override val text: String
): Background, Border, Text {
    companion object
}

data class Color(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Double
) {
    companion object
}

data class Font(
    val size: Int,
    val weight: FontWeight
) {
    companion object
}

enum class FontWeight(
    val wireFormat: String
) {
    UltraLight("ULTRALIGHT"),
    Thin("THIN"),
    Light("LIGHT"),
    Regular("REGULAR"),
    Medium("MEDIUM"),
    SemiBold("SEMIBOLD"),
    Bold("BOLD"),
    Heavy("HEAVY"),
    Black("BLACK");

    companion object
}

enum class HorizontalAlignment(
    val wireFormat: String
) {
    Center("CENTER"),
    Left("LEFT"),
    Right("RIGHT"),
    Fill("FILL");

    companion object
}

data class Image(
    val height: Int,
    val isURLOptimizationEnabled: Boolean,
    val name: String,
    val size: Int,
    val width: Int,
    val url: URI
) {
    companion object
}



data class Insets(
    val bottom: Int,
    val left: Int,
    val right: Int,
    val top: Int
) {
    companion object
}

data class Length(
    val unit: UnitOfMeasure,
    val value: Double
) {
    companion object
}

data class Offsets(
    val bottom: Length,
    val center: Length,
    val left: Length,
    val middle: Length,
    val right: Length,
    val top: Length
) {
    companion object
}

enum class Position(
    val wireFormat: String
) {
    Stacked("STACKED"),
    Floating("FLOATING");

    companion object
}

data class Row(
    val autoHeight: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    val blocks: List<Block>,
    val height: Length,
    val id: ID
): Background {
    companion object
}

data class Screen(
    val autoColorStatusBar: Boolean,
    override val backgroundColor: Color,
    override val backgroundContentMode: BackgroundContentMode,
    override val backgroundImage: Image?,
    override val backgroundScale: BackgroundScale,
    val id: ID,
    val isStretchyHeaderEnabled: Boolean,
    val rows: List<Row>,
    val statusBarStyle: StatusBarStyle,
    val statusBarColor: Color,
    val titleBarBackgroundColor: Color,
    val titleBarButtons: TitleBarButtons,
    val titleBarButtonColor: Color,
    val titleBarText: String,
    val titleBarTextColor: Color,
    val useDefaultTitleBarStyle: Boolean
): Background {
    companion object
}

enum class StatusBarStyle(
    val wireFormat: String
) {
    Dark("DARK"),
    Light("LIGHT");

    companion object
}

interface Text {
    val text: String
    val textAlignment: TextAlignment
    val textColor: Color
    val textFont: Font
}

enum class TextAlignment(
    val wireFormat: String
) {
    Center("CENTER"),
    Left("LEFT"),
    Right("RIGHT");

    companion object
}

enum class TitleBarButtons(
    val wireFormat: String
) {
    Close("CLOSE"),
    Back("BACK"),
    Both("BOTH");

    companion object
}

enum class UnitOfMeasure(
    val wireFormat: String
) {
    Points("POINTS"),
    Percentage("PERCENTAGE");

    companion object
}

enum class VerticalAlignment(
    val wireFormat: String
) {
    Bottom("BOTTOM"),
    Middle("MIDDLE"),
    Fill("FILL"),
    Top("TOP");

    companion object
}