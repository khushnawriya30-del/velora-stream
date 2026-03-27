package com.cinevault.app.ui.navigation

import com.cinevault.app.data.model.HomeSectionDto

/**
 * Simple in-memory holder to pass section data between screens
 * without complex serialization through nav args.
 */
object SectionDataHolder {
    private var _section: HomeSectionDto? = null

    fun set(section: HomeSectionDto) {
        _section = section
    }

    fun get(): HomeSectionDto? {
        return _section
    }

    fun clear() {
        _section = null
    }
}
