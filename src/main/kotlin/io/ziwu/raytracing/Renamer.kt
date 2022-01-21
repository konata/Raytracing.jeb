package io.ziwu.raytracing

import com.pnfsoftware.jeb.core.actions.*
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit

object Renamer {
    fun rename(dex: IDexUnit, newly: String, id: Long) {
        val data = ActionRenameData()
        data.newName = newly
        val request = ActionContext(dex, Actions.RENAME, id)
        if (dex.prepareExecution(request, data)) {
            dex.executeAction(request, data, false)
        }
    }

    fun move(dex: IDexUnit, path: String, id: Long) {
        val data = ActionMoveToPackageData()
        data.dstPackageFqname = path
        val request = ActionContext(dex, Actions.MOVE_TO_PACKAGE, id)
        if (dex.prepareExecution(request, data)) {
            dex.executeAction(request, data, false)
        }
    }

    fun createPackage(dex: IDexUnit, path: String) {
        val data = ActionCreatePackageData()
        data.fqname = path
        val request = ActionContext(dex, Actions.CREATE_PACKAGE, 0)
        if (dex.prepareExecution(request, data)) {
            dex.executeAction(request, data, false)
        }
    }
}