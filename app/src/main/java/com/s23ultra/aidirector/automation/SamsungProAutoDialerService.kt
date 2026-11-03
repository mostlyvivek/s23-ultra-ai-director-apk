package com.s23ultra.aidirector.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.s23ultra.aidirector.model.S23ProRecipe

class SamsungProAutoDialerService : AccessibilityService() {

    companion object {
        var instance: SamsungProAutoDialerService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun applyRecipeToSamsungCamera(recipe: S23ProRecipe) {
        val root = rootInActiveWindow ?: return

        // 1. Switch Lens ("0.6", "1", "3", "10")
        val cleanLensNumber = recipe.recommendedLens.replace("x", "")
        clickNodeByText(root, cleanLensNumber)

        // 2. Select ISO Dial
        if (clickNodeByText(root, "ISO")) {
            clickNodeByText(root, recipe.iso.toString())
        }

        // 3. Select SPEED Dial
        if (clickNodeByText(root, "SPEED")) {
            clickNodeByText(root, recipe.shutterSpeed)
        }

        // 4. Select WB Dial
        if (clickNodeByText(root, "WB")) {
            clickNodeByText(root, "${recipe.kelvinWb}K")
        }
    }

    private fun clickNodeByText(root: AccessibilityNodeInfo, textQuery: String): Boolean {
        val matches = root.findAccessibilityNodeInfosByText(textQuery)
        for (node in matches) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }
}
