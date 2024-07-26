package com.auto.myapplication.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


/**
 *    author : fengqiao
 *    date   : 2021/11/29 10:20
 *    desc   :
 */
class MyAccessibilityService : AccessibilityService() {

    companion object {
        val TAG = MyAccessibilityService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        val className = event.className
        val sourceViewId = event.source?.viewIdResourceName
//        Log.e("TAG", "onAccessibilityEvent$event")
//        if (sourceViewId == "com.transsnet.palmpay:id/vp_main") return
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchWindowStateChange(event)
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            dispatchWindowContentChange(event)
        }
    }

    private fun findNodeInfo(viewId: String, parent: AccessibilityNodeInfo? = rootInActiveWindow) : AccessibilityNodeInfo? {
        return parent?.findAccessibilityNodeInfosByViewId(viewId)?.getOrNull(0)
    }

    private var isScrollComplete = false
    private var groupName : CharSequence? = null
    private var isDoing = false

    private fun dispatchWindowStateChange(event: AccessibilityEvent) {
        Log.e(TAG, "dispatchWindowStateChange ${event.className}")
        if (event.className == "com.tencent.mm.ui.LauncherUI" && event.source != null) {
            onLauncherUI()
        } else if (event.className == "com.tencent.mm.ui.conversation.ConvBoxServiceConversationUI" && event.source != null) {
            onConvBoxServiceConversationUI()
        } else if (event.className == "com.tencent.mm.chatroom.ui.ChatroomInfoUI" && event.source != null) {
            onChatroomInfoUI()
        } else if (event.className == "yj4.o3") {
            findNodeInfo("com.tencent.mm:id/b0f")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            isDoing = true
        }
    }

    private fun onLauncherUI() {
        val obn =
            findNodeInfo("com.tencent.mm:id/obn")?.text
        if (groupName != null && obn?.startsWith(groupName.toString()) == true) {
            findNodeInfo("com.tencent.mm:id/fq")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e(TAG, "real ConversationUI click from onLauncherUI")
            return
        }
        val cj1List =
            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cj1") ?: return
        for (cj1Node in cj1List) {
            val text = findNodeInfo("com.tencent.mm:id/kbq", cj1Node)?.text?.toString()
            /*if (text == "折叠的群聊") {
                cj1Node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isDoing = false
                return
            } else*/ if(text?.contains("语音红包") == true || text?.contains("语音对接") == true) {
                groupName = text.replace("…", "")
                cj1Node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isDoing = false
                return
            }
        }
        val path = Path().apply {
            moveTo(300F, 800F)
            lineTo(300F, 100F)
        }
        val builder = GestureDescription.Builder();
        val description = builder
            .addStroke(GestureDescription.StrokeDescription(path, 20L, 100L))
            .build()
        val ret = dispatchGesture(description, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.e(TAG, "onLauncherUI onCompleted")
                isScrollComplete = true
                onLauncherUI()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "onLauncherUI onCancelled")
            }
        }, null)
    }

    private fun onChatroomInfoUI() {
        if (isDoing) return
        val path = Path().apply {
            moveTo(300F, 800F)
            lineTo(300F, 100F)
        }
        val builder = GestureDescription.Builder();
        val description = builder
            .addStroke(GestureDescription.StrokeDescription(path, 20L, 100L))
            .build()
        val ret = dispatchGesture(description, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.e(TAG, "onChatroomInfoUI onCompleted")
                isScrollComplete = true
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "onChatroomInfoUI onCancelled")
            }
        }, null)
        Log.e("TAG", "onChatroomInfoUI scroll => $ret")
    }

    private fun onConvBoxServiceConversationUI() {
        if (findNodeInfo("com.tencent.mm:id/obn")?.text?.toString() == "折叠的群聊") {
            val cj0Node = findNodeInfo("com.tencent.mm:id/cj0")
            groupName = findNodeInfo("com.tencent.mm:id/kbq", cj0Node)?.text
            cj0Node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            groupName = null
            findNodeInfo("com.tencent.mm:id/fq")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e(TAG, "real ConversationUI click")
        }
    }

    private fun dispatchWindowContentChange(event: AccessibilityEvent) {
        Log.e(TAG, "dispatchWindowContentChange => " + event)
        rootInActiveWindow ?: return
        if (groupName != null && findNodeInfo("com.tencent.mm:id/obn")?.text?.startsWith(groupName.toString()) == true) {
            groupName = null
            findNodeInfo("com.tencent.mm:id/fq")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else if (isScrollComplete) {
//            Log.e("TAG", "dispatchWindowContentChange " + findNodeInfo("com.tencent.mm:id/o3b", event.source))
            val findNodeInfo = findNodeInfo("com.tencent.mm:id/o3b", event.source) ?: return
            isScrollComplete = false
            findNodeInfo.parent?.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val rect = Rect()
            findNodeInfo.getBoundsInScreen(rect)
            tap(rect.left, rect.top)
            Log.e(TAG, "click")
        }
    }

    private fun tap(x: Int, y: Int) {
        val builder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        builder.addStroke(GestureDescription.StrokeDescription(path, 0L, 500L))
        val gesture = builder.build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.e(TAG, "tap ${x},${y} onCompleted")
                isScrollComplete = true
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "tap ${x},${y} onCancelled")
            }
        }, null)
    }


    override fun onInterrupt() {

    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

}