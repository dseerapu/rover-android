package io.rover.rover.core.data.graphql.operations

import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.data.NetworkRequest
import io.rover.rover.core.data.http.WireEncoderInterface
import org.json.JSONObject

class FetchExperienceRequest(
    val queryIdentifier: ExperienceQueryIdentifier
) : NetworkRequest<Experience> {
    override val operationName: String = "FetchExperience"

    override val query: String = """
        query FetchExperience(${"\$"}id: ID, ${"\$"}campaignId: ID, ${"\$"}url: String) {
            experience(id: ${"\$"}id, campaignId: ${"\$"}campaignId, url: ${"\$"}url) {
                homeScreenId
                id
                customKeys
                campaignId
                screens {
                    ...backgroundFields
                    experienceId
                    id
                    isStretchyHeaderEnabled
                    customKeys
                    rows {
                        ...backgroundFields
                        autoHeight
                        customKeys
                        blocks {
                            __typename
                            action {
                                __typename
                                ... on GoToScreenAction {
                                    experienceId
                                    screenId
                                }
                                ... on OpenUrlAction {
                                    url
                                }
                            }
                            autoHeight
                            experienceId
                            height {
                                value
                                unit
                            }
                            id
                            insets {
                                bottom
                                left
                                right
                                top
                            }
                            horizontalAlignment
                            offsets {
                                bottom {
                                    value
                                    unit
                                }
                                center {
                                    value
                                    unit
                                }
                                left {
                                    value
                                    unit
                                }
                                middle {
                                    value
                                    unit
                                }
                                right {
                                    value
                                    unit
                                }
                                top {
                                    value
                                    unit
                                }
                            }
                            opacity
                            position
                            rowId
                            screenId
                            verticalAlignment
                            customKeys
                            width {
                                value
                                unit
                            }
                            ... on Background {
                                ...backgroundFields
                            }
                            ... on Border {
                                ...borderFields
                            }
                            ... on Text {
                                ...textFields
                            }
                            ... on BarcodeBlock {
                                barcodeScale
                                barcodeText
                                barcodeFormat
                            }
                            ... on ButtonBlock {
                                disabled {
                                    ...buttonStateFields
                                }
                                normal {
                                    ...buttonStateFields
                                }
                                highlighted {
                                    ...buttonStateFields
                                }
                                selected {
                                    ...buttonStateFields
                                }
                            }
                            ... on ImageBlock {
                                image {
                                    height
                                    isURLOptimizationEnabled
                                    name
                                    size
                                    width
                                    url
                                }
                            }
                            ... on WebViewBlock {
                                isScrollingEnabled
                                url
                            }
                        }
                        experienceId
                        height {
                            value
                            unit
                        }
                        id
                        screenId
                    }
                    statusBarStyle
                    statusBarColor {
                        red
                        green
                        blue
                        alpha
                    }
                    titleBarBackgroundColor {
                        red
                        green
                        blue
                        alpha
                    }
                    titleBarButtons
                    titleBarButtonColor {
                        red
                        green
                        blue
                        alpha
                    }
                    titleBarText
                    titleBarTextColor {
                        red
                        green
                        blue
                        alpha
                    }
                    useDefaultTitleBarStyle
                }
            }
        }

        fragment buttonStateFields on ButtonState {
            ...backgroundFields
            ...borderFields
            ...textFields
        }

        fragment backgroundFields on Background {
            backgroundColor {
                red
                green
                blue
                alpha
            }
            backgroundContentMode
            backgroundImage {
                height
                name
                size
                width
                url
            }
            backgroundScale
        }

        fragment borderFields on Border {
            borderColor {
                red
                green
                blue
                alpha
            }
            borderRadius
            borderWidth
        }

        fragment textFields on Text {
            text
            textAlignment
            textColor {
                red
                green
                blue
                alpha
            }
            textFont {
                size
                weight
            }
        }
    """
    override val variables: JSONObject = JSONObject().apply {
        when(queryIdentifier) {
            is ExperienceQueryIdentifier.ById -> {
                put("id", queryIdentifier.id)
                put("campaignId", queryIdentifier.campaignId)
                put("url", JSONObject.NULL)
            }
            is ExperienceQueryIdentifier.ByUniversalLink -> {
                put("url", queryIdentifier.uri)
                put("id", JSONObject.NULL)
                put("campaignId", JSONObject.NULL)
            }
        }
    }

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): Experience {
        return wireEncoder.decodeExperience(
            responseObject.getJSONObject("data").getJSONObject("experience")
        )
    }

    sealed class ExperienceQueryIdentifier {
        /**
         * Experiences may be started by just their ID, and a possibly associated campaign Id
         * such that campaign-specific parameters may be interpolated into the Experience.
         *
         * (This method is typically used when experiences are started from a deep link or
         * progammatically.)
         */
        data class ById(val id: String, val campaignId: String? = null): ExperienceQueryIdentifier()

        /**
         * Experiences may be started from a universal link.  The link itself may ultimately, but
         * opaquely, address the experience and a possibly associated campaign, but it is up to the
         * cloud API to resolve it.
         *
         * (This method is typically used when experiences are started from external sources,
         * particularly email, social, external apps, and other services integrated into the app).
         */
        data class ByUniversalLink(val uri: String): ExperienceQueryIdentifier()
    }
}
