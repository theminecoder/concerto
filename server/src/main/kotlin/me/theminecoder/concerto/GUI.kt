@file:Suppress(
    "DuplicatedCode", "UNUSED_PARAMETER", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package me.theminecoder.concerto

import java.util.*
import java.util.function.Consumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

abstract class GUI {
    protected var inventory: InventoryProxy
    private var bukkitInventory: Inventory
    private var populated = false
    private var invCheckOverride = false
    private var allowDrag = false
    protected var isAllowShiftClicking = false
    private var updaterTask: BukkitRunnable? = null

    constructor(title: Component, size: Int) {
        this.bukkitInventory = Bukkit.createInventory(null, getInvSizeForCount(size), title)
        this.inventory =
            InventoryProxy(
                this.bukkitInventory, Bukkit.createInventory(null, getInvSizeForCount(size), title))
        Bukkit.getPluginManager().registerEvents(GUIEvents(this), Concerto.PLUGIN)
    }

    constructor(title: Component, type: InventoryType) {
        this.bukkitInventory = Bukkit.createInventory(null, type, title)
        this.inventory =
            InventoryProxy(this.bukkitInventory, Bukkit.createInventory(null, type, title))
        Bukkit.getPluginManager().registerEvents(GUIEvents(this), Concerto.PLUGIN)
    }

    constructor(bukkitInventory: Inventory) {
        this.bukkitInventory = bukkitInventory
        this.inventory = InventoryProxy(bukkitInventory, bukkitInventory)
        Bukkit.getPluginManager().registerEvents(GUIEvents(this), Concerto.PLUGIN)
    }

    fun open(p: Player) {
        try {
            if (!populated) {
                populate()
                this.inventory.apply()
                populated = true
            }
            openInventory(p)
        } catch (e: Throwable) {
            throwError(e)
        }
    }

    fun close() {
        ArrayList<HumanEntity>(this.inventory.viewers)
            .forEach(Consumer { obj: HumanEntity -> obj.closeInventory() })
    }

    protected fun openInventory(p: Player) {
        p.openInventory(this.inventory)
    }

    protected abstract fun onPlayerClick(event: InventoryClickEvent)
    protected fun onTickUpdate() {}
    protected fun onPlayerCloseInv() {}
    protected fun onPlayerDrag(event: InventoryDragEvent) {}
    protected fun getInvSizeForCount(count: Int): Int {
        var size = count / 9 * 9
        if (count % 9 > 0) size += 9
        if (size < 9) return 9
        return if (size > 54) 54 else size
    }

    fun setInvCheckOverride(invCheckOverride: Boolean) {
        this.invCheckOverride = invCheckOverride
    }

    protected abstract fun populate()
    protected fun repopulate() {
        try {
            this.inventory.clear()
            populate()
            this.inventory.apply()
            populated = true
        } catch (e: Throwable) {
            throwError(e)
        }
    }

    protected fun setUpdateTicks(ticks: Int) {
        this.setUpdateTicks(ticks, false)
    }

    protected fun setUpdateTicks(ticks: Int, sync: Boolean) {
        if (updaterTask != null) {
            updaterTask!!.cancel()
            updaterTask = null
        }
        updaterTask = GUIUpdateTask(this)
        if (sync) {
            (updaterTask as GUIUpdateTask).runTaskTimer(Concerto.PLUGIN, 0, ticks.toLong())
        } else {
            (updaterTask as GUIUpdateTask).runTaskTimerAsynchronously(
                Concerto.PLUGIN, 0, ticks.toLong())
        }
    }

    protected fun scheduleOpen(gui: GUI, player: Player) {
        Bukkit.getScheduler().runTask(Concerto.PLUGIN, Runnable { gui.open(player) })
    }

    protected fun setAllowDrag(allowDrag: Boolean) {
        this.allowDrag = allowDrag
    }

    private fun throwError(e: Throwable) {
        e.printStackTrace()
    }

    private inner class GUIEvents(private val gui: GUI) : Listener {
        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            try {
                if (gui.inventory.viewers.contains(event.whoClicked)) {
                    val deniedActions: MutableList<InventoryAction> =
                        ArrayList(
                            Arrays.asList(
                                InventoryAction.CLONE_STACK,
                                InventoryAction.COLLECT_TO_CURSOR,
                                InventoryAction.UNKNOWN))
                    if (!isAllowShiftClicking) {
                        deniedActions.add(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                    }
                    if (deniedActions.contains(event.action)) {
                        event.isCancelled = true
                    }
                    if (!isAllowShiftClicking && event.click.isShiftClick) {
                        event.isCancelled = true
                    }
                }
                if (!invCheckOverride && (event.inventory != gui.inventory)) return
                event.isCancelled = true
                if (event.whoClicked !is Player) return
                gui.onPlayerClick(event)
            } catch (e: Throwable) {
                gui.throwError(e)
            }
        }

        @EventHandler
        fun onInventoryClose(event: InventoryCloseEvent) {
            if (event.inventory != gui.inventory) return
            if (inventory.viewers.size <= 1) {
                HandlerList.unregisterAll(this)
                try {
                    gui.onPlayerCloseInv()
                } catch (e: Throwable) {
                    gui.throwError(e)
                }
                if (gui.updaterTask != null) {
                    gui.updaterTask!!.cancel()
                }
            }
        }

        @EventHandler
        fun onInventoryDrag(event: InventoryDragEvent) {
            try {
                if (event.inventory != gui.inventory) return
                if (!allowDrag) {
                    event.isCancelled = true
                } else {
                    gui.onPlayerDrag(event)
                }
            } catch (e: Throwable) {
                gui.throwError(e)
            }
        }
    }

    private inner class GUIUpdateTask(private val gui: GUI) : BukkitRunnable() {
        override fun run() {
            try {
                gui.repopulate()
                gui.onTickUpdate()
            } catch (e: Throwable) {
                gui.throwError(e)
            }
        }
    }
}

class InventoryProxy
internal constructor(private val mainInventory: Inventory, private val proxyInventory: Inventory) :
    Inventory {
    fun apply() {
        mainInventory.contents = proxyInventory.contents
    }

    override fun getSize(): Int {
        return proxyInventory.size
    }

    override fun getMaxStackSize(): Int {
        return proxyInventory.maxStackSize
    }

    override fun setMaxStackSize(i: Int) {
        proxyInventory.maxStackSize = i
    }

    override fun getItem(i: Int): ItemStack? {
        return proxyInventory.getItem(i)
    }

    override fun setItem(i: Int, itemStack: ItemStack?) {
        proxyInventory.setItem(i, itemStack)
    }

    @Throws(IllegalArgumentException::class)
    override fun addItem(vararg itemStacks: ItemStack): HashMap<Int, ItemStack> {
        return proxyInventory.addItem(*itemStacks)
    }

    @Throws(IllegalArgumentException::class)
    override fun removeItem(vararg itemStacks: ItemStack): HashMap<Int, ItemStack> {
        return proxyInventory.removeItem(*itemStacks)
    }

    @Throws(IllegalArgumentException::class)
    override fun removeItemAnySlot(vararg itemStacks: ItemStack): HashMap<Int, ItemStack> {
        return mainInventory.removeItemAnySlot(*itemStacks)
    }

    override fun getContents(): Array<ItemStack> {
        return proxyInventory.contents
    }

    @Throws(IllegalArgumentException::class)
    override fun setContents(itemStacks: Array<ItemStack>) {
        proxyInventory.contents = itemStacks
    }

    override fun getStorageContents(): Array<ItemStack> {
        return arrayOf()
    }

    @Throws(IllegalArgumentException::class)
    override fun setStorageContents(itemStacks: Array<ItemStack>) {}

    @Throws(IllegalArgumentException::class)
    override fun contains(material: Material): Boolean {
        return proxyInventory.contains(material)
    }

    override fun contains(itemStack: ItemStack?): Boolean {
        return proxyInventory.contains(itemStack)
    }

    @Throws(IllegalArgumentException::class)
    override fun contains(material: Material, i: Int): Boolean {
        return proxyInventory.contains(material, i)
    }

    override fun contains(itemStack: ItemStack?, i: Int): Boolean {
        return proxyInventory.contains(itemStack, i)
    }

    override fun containsAtLeast(itemStack: ItemStack?, i: Int): Boolean {
        return proxyInventory.containsAtLeast(itemStack, i)
    }

    @Throws(IllegalArgumentException::class)
    override fun all(material: Material): HashMap<Int, out ItemStack> {
        return proxyInventory.all(material)
    }

    override fun all(itemStack: ItemStack?): HashMap<Int, out ItemStack> {
        return proxyInventory.all(itemStack)
    }

    @Throws(IllegalArgumentException::class)
    override fun first(material: Material): Int {
        return proxyInventory.first(material)
    }

    override fun first(itemStack: ItemStack): Int {
        return proxyInventory.first(itemStack)
    }

    override fun firstEmpty(): Int {
        return proxyInventory.firstEmpty()
    }

    override fun isEmpty(): Boolean {
        return proxyInventory.isEmpty
    }

    @Throws(IllegalArgumentException::class)
    override fun remove(material: Material) {
        proxyInventory.remove(material)
    }

    override fun remove(itemStack: ItemStack) {
        proxyInventory.remove(itemStack)
    }

    override fun clear(i: Int) {
        proxyInventory.clear(i)
    }

    override fun clear() {
        proxyInventory.clear()
    }

    override fun close(): Int {
        return proxyInventory.close()
    }

    override fun getViewers(): List<HumanEntity> {
        return mainInventory.viewers
    }

    override fun getType(): InventoryType {
        return mainInventory.type
    }

    override fun getHolder(): InventoryHolder? {
        return mainInventory.holder
    }

    override fun getHolder(useSnapshot: Boolean): InventoryHolder? {
        return null
    }

    override fun iterator(): MutableListIterator<ItemStack> {
        return proxyInventory.iterator()
    }

    override fun iterator(i: Int): ListIterator<ItemStack> {
        return proxyInventory.iterator(i)
    }

    override fun getLocation(): Location {
        return Location(Bukkit.getWorlds()[0], 0.0, 0.0, 0.0)
    }
}

abstract class PagedGUI(title: Component, size: Int) : GUI(title, size) {
    private var page = 0
    private var headerRows = 0
    private val internalSize: Int = getInvSizeForCount(size - 9)
    protected abstract val icons: List<ItemStack>

    protected abstract fun onPlayerClickIcon(event: InventoryClickEvent)
    protected abstract fun populateSpecial()
    fun setHeaderRows(headerRows: Int) {
        this.headerRows = headerRows
    }

    override fun onPlayerClick(event: InventoryClickEvent) {
        val items = icons
        val pageSize = internalSize - headerRows * 9
        var pages = items.size / pageSize
        if (items.size % pageSize > 0) pages++
        if (event.rawSlot == internalSize + 3 && page > 0) {
            page--
            repopulate()
            return
        }
        if (event.rawSlot == internalSize + 5 && page + 1 < pages) {
            page++
            repopulate()
            return
        }
        if (event.rawSlot == internalSize + 4) {
            return
        }
        onPlayerClickIcon(event)
    }

    override fun populate() {
        val items = icons
        val pageSize = internalSize - headerRows * 9
        var pages = items.size / pageSize
        if (items.size % pageSize > 0) pages++
        if (page > pages) {
            page--
            repopulate()
            return
        }
        var slot = headerRows * 9
        for (i in page * pageSize until items.size) {
            if (slot > pageSize + headerRows * 9 - 1) break
            this.inventory.setItem(slot++, items[i])
        }
        if (page > 0) {
            this.inventory.setItem(
                internalSize + 3,
                item(Material.ARROW, text("<- Previous Page", NamedTextColor.WHITE)))
        }
        if (page + 1 < pages) {
            this.inventory.setItem(
                internalSize + 5, item(Material.ARROW, text("Next Page ->", NamedTextColor.WHITE)))
        }
        this.inventory.setItem(
            internalSize + 4,
            item(Material.MAP, text("Page ${page + 1}/$pages", NamedTextColor.WHITE)))
        populateSpecial()
    }
}
