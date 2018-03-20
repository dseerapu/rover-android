@file:JvmName("Experience")

package io.rover.rover.core.data.graphql.operations.data

import io.rover.rover.core.data.domain.Background
import io.rover.rover.core.data.domain.BackgroundContentMode
import io.rover.rover.core.data.domain.BackgroundScale
import io.rover.rover.core.data.domain.BarcodeBlock
import io.rover.rover.core.data.domain.BarcodeFormat
import io.rover.rover.core.data.domain.Block
import io.rover.rover.core.data.domain.BlockAction
import io.rover.rover.core.data.domain.Border
import io.rover.rover.core.data.domain.ButtonBlock
import io.rover.rover.core.data.domain.ButtonState
import io.rover.rover.core.data.domain.Color
import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.domain.Font
import io.rover.rover.core.data.domain.FontWeight
import io.rover.rover.core.data.domain.HorizontalAlignment
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.data.domain.Image
import io.rover.rover.core.data.domain.ImageBlock
import io.rover.rover.core.data.domain.Insets
import io.rover.rover.core.data.domain.Length
import io.rover.rover.core.data.domain.Offsets
import io.rover.rover.core.data.domain.Position
import io.rover.rover.core.data.domain.RectangleBlock
import io.rover.rover.core.data.domain.Row
import io.rover.rover.core.data.domain.Screen
import io.rover.rover.core.data.domain.StatusBarStyle
import io.rover.rover.core.data.domain.Text
import io.rover.rover.core.data.domain.TextAlignment
import io.rover.rover.core.data.domain.TextBlock
import io.rover.rover.core.data.domain.TitleBarButtons
import io.rover.rover.core.data.domain.UnitOfMeasure
import io.rover.rover.core.data.domain.VerticalAlignment
import io.rover.rover.core.data.domain.WebViewBlock
import io.rover.rover.core.data.graphql.getObjectIterable
import io.rover.rover.core.data.graphql.putProp
import io.rover.rover.core.data.graphql.safeGetString
import io.rover.rover.core.data.graphql.safeOptString
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

internal fun Experience.Companion.decodeJson(json: JSONObject): Experience {
    return Experience(
        id = ID(json.safeGetString("id")),
        homeScreenId = ID(json.safeGetString("homeScreenId")),
        screens = json.getJSONArray("screens").getObjectIterable().map {
            Screen.decodeJson(it)
        },
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash(),
        campaignId = json.safeOptString("campaignId")
    )
}

internal fun Experience.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Experience::id) { it.rawValue }
        putProp(this@encodeJson, Experience::homeScreenId) { it.rawValue }
        putProp(this@encodeJson, Experience::screens) { JSONArray(it.map { it.encodeJson(this@encodeJson.id.rawValue) }) }
        putProp(this@encodeJson, Experience::customKeys) { it.encodeJson() }
        putProp(this@encodeJson, Experience::campaignId)
    }
}

internal fun Color.Companion.decodeJson(json: JSONObject): Color {
    return Color(
        json.getInt("red"),
        json.getInt("green"),
        json.getInt("blue"),
        json.getDouble("alpha")
    )
}

internal fun Color.encodeJson(): JSONObject {
    return JSONObject().apply {
        listOf(
            Color::red, Color::green, Color::blue, Color::alpha
        ).forEach { putProp(this@encodeJson, it) }
    }
}

internal fun BackgroundContentMode.Companion.decodeJSON(value: String): BackgroundContentMode =
    BackgroundContentMode.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown BackgroundContentMode type '$value'")

internal fun BarcodeFormat.Companion.decodeJson(value: String): BarcodeFormat =
    BarcodeFormat.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown BarcodeFormat value '$value'")

internal fun Image.Companion.optDecodeJSON(json: JSONObject?): Image? = when (json) {
    null -> null
    else -> Image(
        json.getInt("width"),
        json.getInt("height"),
        json.safeGetString("name"),
        json.getInt("size"),
        URI.create(json.safeGetString("url"))
    )
}

internal fun Image?.optEncodeJson(): JSONObject? {
    return this?.let {
        JSONObject().apply {
            listOf(
                Image::height,
                Image::name,
                Image::size,
                Image::width
            ).forEach { putProp(this@optEncodeJson, it) }

            putProp(this@optEncodeJson, Image::url) { it.toString() }
        }
    }
}

internal fun Length.Companion.decodeJson(json: JSONObject): Length {
    return Length(
        UnitOfMeasure.decodeJson(json.safeGetString("unit")),
        json.getDouble("value")
    )
}

internal fun Length.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Length::unit) { it.wireFormat }
        putProp(this@encodeJson, Length::value)
    }
}

internal fun UnitOfMeasure.Companion.decodeJson(value: String): UnitOfMeasure =
    UnitOfMeasure.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown Unit type '$value'")

internal fun Insets.Companion.decodeJson(json: JSONObject): Insets {
    return Insets(
        json.getInt("bottom"),
        json.getInt("left"),
        json.getInt("right"),
        json.getInt("top")
    )
}

internal fun Insets.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Insets::bottom)
        putProp(this@encodeJson, Insets::left)
        putProp(this@encodeJson, Insets::right)
        putProp(this@encodeJson, Insets::top)
    }
}

internal fun Offsets.Companion.decodeJson(json: JSONObject): Offsets {
    return Offsets(
        Length.decodeJson(json.getJSONObject("bottom")),
        Length.decodeJson(json.getJSONObject("center")),
        Length.decodeJson(json.getJSONObject("left")),
        Length.decodeJson(json.getJSONObject("middle")),
        Length.decodeJson(json.getJSONObject("right")),
        Length.decodeJson(json.getJSONObject("top"))
    )
}

internal fun Offsets.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Offsets::bottom) { it.encodeJson() }
        putProp(this@encodeJson, Offsets::center) { it.encodeJson() }
        putProp(this@encodeJson, Offsets::left) { it.encodeJson() }
        putProp(this@encodeJson, Offsets::middle) { it.encodeJson() }
        putProp(this@encodeJson, Offsets::right) { it.encodeJson() }
        putProp(this@encodeJson, Offsets::top) { it.encodeJson() }
    }
}

internal fun Font.Companion.decodeJson(json: JSONObject): Font {
    return Font(
        size = json.getInt("size"),
        weight = FontWeight.decodeJson(json.safeGetString("weight"))
    )
}

internal fun Font.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Font::size)
        putProp(this@encodeJson, Font::weight) { it.wireFormat }
    }
}

internal fun Position.Companion.decodeJson(value: String): Position =
    Position.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown Position type '$value'")

internal fun HorizontalAlignment.Companion.decodeJson(value: String): HorizontalAlignment =
    HorizontalAlignment.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown HorizontalAlignment type '$value'.")

internal fun VerticalAlignment.Companion.decodeJson(value: String): VerticalAlignment =
    VerticalAlignment.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown VerticalAlignment type '$value'.")

internal fun TextAlignment.Companion.decodeJson(value: String): TextAlignment =
    TextAlignment.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown TextAlignment type '$value'.")

internal fun FontWeight.Companion.decodeJson(value: String): FontWeight =
    FontWeight.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown FontWeight type '$value'.")

internal fun BackgroundContentMode.Companion.decodeJson(value: String): BackgroundContentMode =
    BackgroundContentMode.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown BackgroundContentMode type '$value'.")

internal fun BackgroundScale.Companion.decodeJson(value: String): BackgroundScale =
    BackgroundScale.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown BackgroundScale type '$value'.")

internal fun StatusBarStyle.Companion.decodeJson(value: String): StatusBarStyle =
    StatusBarStyle.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown StatusBarStyle type '$value'.")

internal fun TitleBarButtons.Companion.decodeJson(value: String): TitleBarButtons =
    TitleBarButtons.values().firstOrNull { it.wireFormat == value } ?: throw Exception("Unknown StatusBar TitleBarButtonsStyle type '$value'.")

internal fun ButtonState.Companion.decodeJson(json: JSONObject): ButtonState {
    return ButtonState(
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        textAlignment = TextAlignment.decodeJson(json.safeGetString("textAlignment")),
        textColor = Color.decodeJson(json.getJSONObject("textColor")),
        textFont = Font.decodeJson(json.getJSONObject("textFont")),
        text = json.safeGetString("text")
    )
}

internal fun ButtonState.encodeJson(): JSONObject {
    return JSONObject().apply {
        this@encodeJson.encodeBackgroundToJson(this)
        this@encodeJson.encodeBorderToJson(this)
        this@encodeJson.encodeTextToJson(this)
    }
}

internal fun BarcodeBlock.Companion.decodeJson(json: JSONObject): BarcodeBlock {
    return BarcodeBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        barcodeScale = json.getInt("barcodeScale"),
        barcodeText = json.safeGetString("barcodeText"),
        barcodeFormat = BarcodeFormat.decodeJson(json.safeGetString("barcodeFormat")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun Background.encodeBackgroundToJson(json: JSONObject) {
    json.putProp(this, Background::backgroundColor) { it.encodeJson() }
    json.putProp(this, Background::backgroundContentMode) { it.wireFormat }
    json.putProp(this, Background::backgroundImage) { it.optEncodeJson() ?: JSONObject.NULL }
    json.putProp(this, Background::backgroundScale) { it.wireFormat }
}

internal fun Border.encodeBorderToJson(json: JSONObject) {
    json.putProp(this, Border::borderColor) { it.encodeJson() }
    json.putProp(this, Border::borderRadius)
    json.putProp(this, Border::borderWidth)
}

internal fun Text.encodeTextToJson(json: JSONObject) {
    json.putProp(this, Text::text)
    json.putProp(this, Text::textAlignment) { it.wireFormat }
    json.putProp(this, Text::textColor) { it.encodeJson() }
    json.putProp(this, Text::textFont) { it.encodeJson() }
}

internal fun Block.encodeJson(experienceId: String, screenId: String, rowId: String): JSONObject {
    return JSONObject().apply {
        // do common fields
        put("experienceId", experienceId)
        put("screenId", screenId)
        put("rowId", rowId)
        putProp(this@encodeJson, Block::action) { it.optEncodeJson() ?: JSONObject.NULL }
        putProp(this@encodeJson, Block::autoHeight)
        putProp(this@encodeJson, Block::height) { it.encodeJson() }
        putProp(this@encodeJson, Block::id) { it.rawValue }
        putProp(this@encodeJson, Block::insets) { it.encodeJson() }
        putProp(this@encodeJson, Block::horizontalAlignment) { it.wireFormat }
        putProp(this@encodeJson, Block::offsets) { it.encodeJson() }
        putProp(this@encodeJson, Block::opacity)
        putProp(this@encodeJson, Block::position) { it.wireFormat }
        putProp(this@encodeJson, Block::verticalAlignment) { it.wireFormat }
        putProp(this@encodeJson, Block::width) { it.encodeJson() }
        putProp(this@encodeJson, Block::customKeys) { it.encodeJson() }
        put("__typename", when (this@encodeJson) {
            is BarcodeBlock -> {
                putProp(this@encodeJson, BarcodeBlock::barcodeScale)
                putProp(this@encodeJson, BarcodeBlock::barcodeText)
                putProp(this@encodeJson, BarcodeBlock::barcodeFormat) { it.wireFormat }
                this@encodeJson.encodeBackgroundToJson(this)
                this@encodeJson.encodeBorderToJson(this)
                BarcodeBlock.resourceName
            }
            is ButtonBlock -> {
                putProp(this@encodeJson, ButtonBlock::disabled) { it.encodeJson() }
                putProp(this@encodeJson, ButtonBlock::highlighted) { it.encodeJson() }
                putProp(this@encodeJson, ButtonBlock::normal) { it.encodeJson() }
                putProp(this@encodeJson, ButtonBlock::selected) { it.encodeJson() }
                ButtonBlock.resourceName
            }
            is ImageBlock -> {
                putProp(this@encodeJson, ImageBlock::image) { it.optEncodeJson() ?: JSONObject.NULL }
                this@encodeJson.encodeBackgroundToJson(this)
                this@encodeJson.encodeBorderToJson(this)
                ImageBlock.resourceName
            }
            is WebViewBlock -> {
                putProp(this@encodeJson, WebViewBlock::isScrollingEnabled)
                putProp(this@encodeJson, WebViewBlock::url) { it.toString() }
                this@encodeJson.encodeBackgroundToJson(this)
                this@encodeJson.encodeBorderToJson(this)
                WebViewBlock.resourceName
            }
            is RectangleBlock -> {
                this@encodeJson.encodeBackgroundToJson(this)
                this@encodeJson.encodeBorderToJson(this)
                RectangleBlock.resourceName
            }
            is TextBlock -> {
                this@encodeJson.encodeBackgroundToJson(this)
                this@encodeJson.encodeBorderToJson(this)
                this@encodeJson.encodeTextToJson(this)
                TextBlock.resourceName
            }
            else -> throw RuntimeException("Unsupported Block type for serialization")
        })
    }
}

internal fun ButtonBlock.Companion.decodeJson(json: JSONObject): ButtonBlock {
    return ButtonBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        disabled = ButtonState.decodeJson(json.getJSONObject("disabled")),
        highlighted = ButtonState.decodeJson(json.getJSONObject("highlighted")),
        normal = ButtonState.decodeJson(json.getJSONObject("normal")),
        selected = ButtonState.decodeJson(json.getJSONObject("selected")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun RectangleBlock.Companion.decodeJson(json: JSONObject): RectangleBlock {
    return RectangleBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun WebViewBlock.Companion.decodeJson(json: JSONObject): WebViewBlock {
    return WebViewBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        isScrollingEnabled = json.getBoolean("isScrollingEnabled"),
        url = URI.create(json.safeGetString("url")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun TextBlock.Companion.decodeJson(json: JSONObject): TextBlock {
    return TextBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        textAlignment = TextAlignment.decodeJson(json.safeGetString("textAlignment")),
        textColor = Color.decodeJson(json.getJSONObject("textColor")),
        textFont = Font.decodeJson(json.getJSONObject("textFont")),
        text = json.safeGetString("text"),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun ImageBlock.Companion.decodeJson(json: JSONObject): ImageBlock {
    return ImageBlock(
        action = BlockAction.optDecodeJson(json.optJSONObject("action")),
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        borderColor = Color.decodeJson(json.getJSONObject("borderColor")),
        borderRadius = json.getInt("borderRadius"),
        borderWidth = json.getInt("borderWidth"),
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        insets = Insets.decodeJson(json.getJSONObject("insets")),
        horizontalAlignment = HorizontalAlignment.decodeJson(json.safeGetString("horizontalAlignment")),
        offsets = Offsets.decodeJson(json.getJSONObject("offsets")),
        opacity = json.getDouble("opacity"),
        position = Position.decodeJson(json.safeGetString("position")),
        verticalAlignment = VerticalAlignment.decodeJson(json.safeGetString("verticalAlignment")),
        width = Length.decodeJson(json.getJSONObject("width")),
        image = Image.optDecodeJSON(json.optJSONObject("image")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal val BlockAction.OpenUrlAction.Companion.resourceName get() = "OpenUrlAction"
internal val BlockAction.GoToScreenAction.Companion.resourceName get() = "GoToScreenAction"

internal fun BlockAction.Companion.optDecodeJson(json: JSONObject?): BlockAction? {
    if (json == null) return null
    // BlockAction has subclasses, so we need to delegate to the appropriate deserializer for each
    // block action type.

    val typeName = json.safeGetString("__typename")

    return when (typeName) {
        BlockAction.OpenUrlAction.resourceName -> BlockAction.OpenUrlAction(
            url = URI.create(json.safeGetString("url"))
        )
        BlockAction.GoToScreenAction.resourceName -> BlockAction.GoToScreenAction(
            experienceId = ID(json.safeGetString("experienceId")),
            screenId = ID(json.safeGetString("screenId"))
        )
        else -> throw RuntimeException("Unsupported Block Action type '$typeName'.")
    }
}

internal fun BlockAction?.optEncodeJson(): JSONObject? {
    return this?.let {
        JSONObject().apply {
            put("__typename", when (this@optEncodeJson) {
                is BlockAction.OpenUrlAction -> {
                    putProp(this@optEncodeJson, BlockAction.OpenUrlAction::url) { it.toString() }
                    BlockAction.OpenUrlAction.resourceName
                }
                is BlockAction.GoToScreenAction -> {
                    putProp(this@optEncodeJson, BlockAction.GoToScreenAction::experienceId) { it.rawValue }
                    putProp(this@optEncodeJson, BlockAction.GoToScreenAction::screenId) { it.rawValue }
                    BlockAction.GoToScreenAction.resourceName
                }
            })
        }
    }
}

internal val BarcodeBlock.Companion.resourceName get() = "BarcodeBlock"
internal val ButtonBlock.Companion.resourceName get() = "ButtonBlock"
internal val RectangleBlock.Companion.resourceName get() = "RectangleBlock"
internal val WebViewBlock.Companion.resourceName get() = "WebViewBlock"
internal val TextBlock.Companion.resourceName get() = "TextBlock"
internal val ImageBlock.Companion.resourceName get() = "ImageBlock"

internal fun Block.Companion.decodeJson(json: JSONObject): Block {
    // Block has subclasses, so we need to delegate to the appropriate deserializer for each
    // block type.
    val typeName = json.safeGetString("__typename")

    return when (typeName) {
        BarcodeBlock.resourceName -> BarcodeBlock.decodeJson(json)
        ButtonBlock.resourceName -> ButtonBlock.decodeJson(json)
        RectangleBlock.resourceName -> RectangleBlock.decodeJson(json)
        WebViewBlock.resourceName -> WebViewBlock.decodeJson(json)
        TextBlock.resourceName -> TextBlock.decodeJson(json)
        ImageBlock.resourceName -> ImageBlock.decodeJson(json)
        else -> throw RuntimeException("Unsupported Block type '$typeName'.")
    }
}

internal fun Row.Companion.decodeJSON(json: JSONObject): Row {
    return Row(
        autoHeight = json.getBoolean("autoHeight"),
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJSON(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        blocks = json.getJSONArray("blocks").getObjectIterable().map { Block.decodeJson(it) },
        height = Length.decodeJson(json.getJSONObject("height")),
        id = ID(json.safeGetString("id")),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun Row.encodeJson(experienceId: String, screenId: String): JSONObject {
    return JSONObject().apply {
        put("experienceId", experienceId)
        put("screenId", screenId)
        putProp(this@encodeJson, Row::autoHeight)
        putProp(this@encodeJson, Row::backgroundColor) { it.encodeJson() }
        putProp(this@encodeJson, Row::backgroundContentMode) { it.wireFormat }
        putProp(this@encodeJson, Row::backgroundImage) { it.optEncodeJson() ?: JSONObject.NULL }
        putProp(this@encodeJson, Row::backgroundScale) { it.wireFormat }
        putProp(this@encodeJson, Row::blocks) { JSONArray(it.map { it.encodeJson(experienceId, screenId, this@encodeJson.id.rawValue) }) }
        putProp(this@encodeJson, Row::height) { it.encodeJson() }
        putProp(this@encodeJson, Row::id) { it.rawValue }
        putProp(this@encodeJson, Row::customKeys) { it.encodeJson() }
    }
}

internal fun Screen.Companion.decodeJson(json: JSONObject): Screen {
    return Screen(
        backgroundColor = Color.decodeJson(json.getJSONObject("backgroundColor")),
        backgroundContentMode = BackgroundContentMode.decodeJson(json.safeGetString("backgroundContentMode")),
        backgroundImage = Image.optDecodeJSON(json.optJSONObject("backgroundImage")),
        backgroundScale = BackgroundScale.decodeJson(json.safeGetString("backgroundScale")),
        id = ID(json.safeGetString("id")),
        isStretchyHeaderEnabled = json.getBoolean("isStretchyHeaderEnabled"),
        rows = json.getJSONArray("rows").getObjectIterable().map {
            Row.decodeJSON(it)
        },
        statusBarStyle = StatusBarStyle.decodeJson(json.safeGetString("statusBarStyle")),
        statusBarColor = Color.decodeJson(json.getJSONObject("statusBarColor")),
        titleBarBackgroundColor = Color.decodeJson(json.getJSONObject("titleBarBackgroundColor")),
        titleBarButtons = TitleBarButtons.decodeJson(json.safeGetString("titleBarButtons")),
        titleBarButtonColor = Color.decodeJson(json.getJSONObject("titleBarButtonColor")),
        titleBarText = json.safeGetString("titleBarText"),
        titleBarTextColor = Color.decodeJson(json.getJSONObject("titleBarTextColor")),
        useDefaultTitleBarStyle = json.getBoolean("useDefaultTitleBarStyle"),
        customKeys = json.getJSONObject("customKeys").toFlatAttributesHash()
    )
}

internal fun Screen.encodeJson(experienceId: String): JSONObject {
    return JSONObject().apply {
        val primitiveProps = listOf(
            Screen::isStretchyHeaderEnabled,
            Screen::titleBarText,
            Screen::useDefaultTitleBarStyle
        )

        primitiveProps.forEach { putProp(this@encodeJson, it) }
        put("experienceId", experienceId)

        putProp(this@encodeJson, Screen::backgroundColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::backgroundContentMode) { it.wireFormat }
        putProp(this@encodeJson, Screen::backgroundImage) { it.optEncodeJson() ?: JSONObject.NULL }
        putProp(this@encodeJson, Screen::backgroundScale) { it.wireFormat }
        putProp(this@encodeJson, Screen::id) { it.rawValue }
        putProp(this@encodeJson, Screen::rows) { JSONArray(it.map { it.encodeJson(experienceId, this@encodeJson.id.rawValue) }) }
        putProp(this@encodeJson, Screen::statusBarStyle) { it.wireFormat }
        putProp(this@encodeJson, Screen::statusBarColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::titleBarBackgroundColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::titleBarButtons) { it.wireFormat }
        putProp(this@encodeJson, Screen::titleBarButtonColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::titleBarTextColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::titleBarBackgroundColor) { it.encodeJson() }
        putProp(this@encodeJson, Screen::useDefaultTitleBarStyle)
        putProp(this@encodeJson, Screen::customKeys) { it.encodeJson() }
    }
}

