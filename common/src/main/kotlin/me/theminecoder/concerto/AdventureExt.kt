package me.theminecoder.concerto

import net.kyori.adventure.extra.kotlin.keybind
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.extra.kotlin.translatable
import net.kyori.adventure.text.*
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * Creates a [TextComponent] using the given [content] and optional [builder].
 *
 * Prefer this over extra-kotlin's builder function, which requires you to specify the [content]
 * inside the builder.
 */
public fun text(content: String, builder: TextComponent.Builder.() -> Unit = {}): TextComponent =
    text {
    content(content)
    builder(this)
}

/** Creates a [TextComponent] using the given [content] and optional [color] & [decorations]. */
public fun text(
    content: String,
    color: TextColor,
    vararg decorations: TextDecoration
): TextComponent = text {
    content(content)
    color(color)
    decorations.forEach(this::decorate)
}

/**
 * Creates a [TranslatableComponent] using the given translation [key] and optional [builder].
 *
 * Prefer this over extra-kotlin's builder function, which requires you to specify the [key] inside
 * the builder.
 */
public fun translatable(
    key: String,
    builder: TranslatableComponent.Builder.() -> Unit = {}
): TranslatableComponent = translatable {
    key(key)
    builder(this)
}

/**
 * Creates a [KeybindComponent] using the given [keybind] and optional [builder].
 *
 * Prefer this over extra-kotlin's builder function, which requires you to specify the [keybind]
 * inside the builder.
 */
public fun keybind(
    keybind: String,
    builder: KeybindComponent.Builder.() -> Unit = {}
): KeybindComponent = keybind {
    keybind(keybind)
    builder(this)
}

/**
 * Appends [that] component to [this] component, returning a component of the same type as [this].
 */
public operator fun <T : ScopedComponent<T>> T.plus(that: ComponentLike): T = this.append(that)
