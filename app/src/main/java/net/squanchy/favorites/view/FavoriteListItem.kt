package net.squanchy.favorites.view

import net.squanchy.schedule.domain.view.Event
import org.joda.time.LocalDate

sealed class FavoriteListItem {

    abstract val id: Long

    abstract val type: Type

    data class Header(val date: LocalDate) : FavoriteListItem() {

        override val id: Long
            get() = date.toDateTimeAtCurrentTime().millis // We're relatively sure this won't clash with event IDs

        override val type: Type
            get() = Type.HEADER
    }

    data class Favorite(val event: Event) : FavoriteListItem() {

        override val id: Long
            get() = event.numericId

        override val type: Type
            get() = Type.FAVOURITE
    }

    enum class Type {
        HEADER,
        FAVOURITE
    }
}
