package com.example.test

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale

class PermissionsHelper {

    private var permissionList = listOf(
        PermissionsDataList(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            "Геолокация",
            "Нужна для определения ближайшего СТО или другой организации.",
            100
        ),
        PermissionsDataList(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Точная геолокация",
            "Нужна для точного определения ближайшего СТО или другой организации.",
            101
        ),
        PermissionsDataList(
            Manifest.permission.CAMERA,
            "Камера",
            "Нужна для того, чтобы сделать фото автомобиля.",
            102
        ),
        PermissionsDataList(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "Хранение файлов",
            "Нужно чтобы хранить фото автомобиля.",
            103
        ),
        PermissionsDataList(
            Manifest.permission.INTERNET,
            "Доступ в интернет",
            "Нужен для синхронизации Ваших данных",
            104
        ),
        PermissionsDataList(
            Manifest.permission.ACCESS_NETWORK_STATE,
            "Информация о соединении",
            "Нужна чтобы при плохом соединении не нагружать Ваш телефон.",
            105
        ),
        PermissionsDataList(
            Manifest.permission.WRITE_CALENDAR,
            "Календарь",
            "Нужен чтобы добавлять заметки о предстоящем ТО автомобиля.",
            106
        ),
    )

    private var permissionListNext = 1
    private var permissionListMax = permissionList.size-1

    private var lambda: (() -> Unit)? = null

     private fun showPermissionRequestExplanation(
        ctx: Context,
        permission: String,
        message: String,
        ok: (() -> Unit)? = null,
        cancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(ctx).apply {
            setTitle(permission)
            setMessage(message)
            setCancelable(true)
            setNegativeButton(android.R.string.cancel) { _, _ -> cancel?.invoke() }
            setPositiveButton(android.R.string.ok) { _, _ -> ok?.invoke() }
        }.show()
    }

    private fun getPermissionIndex(listArgument: List<PermissionsDataList>, code: Int): Int {
        var i = 0
        var ret = -1
        listArgument.forEach{ (_, _, _, permCode) ->
            if (permCode == code) {
                ret = i
            }
            i++
        }
        return ret
    }

    // Вызывается при запуске приложения
    fun requestAppPermission(ctx: Activity, lambda: (() -> Unit)? = null){

        if (lambda!=null) this.lambda = lambda

        if (permissionListNext>permissionListMax) {
            this.lambda?.invoke()
            return
        }

        val permission = this.permissionList[permissionListNext]

        Log.d("***", "Check "+permission.permNativeName)
        when {
            ctx.checkSelfPermission(permission.permName) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("***", permission.permNativeName+" is granted!")
                // Разрешение получено
                // Рекурсивно вызываем для следующего разрешения
                this.permissionListNext++
                this.requestAppPermission(ctx)
            }
            ctx.shouldShowRequestPermissionRationale(permission.permName) -> {
                Log.d("***", permission.permNativeName+" is denied!")
                // В прошлый раз пользователь просто отказался от разрешения
                showPermissionRequestExplanation(ctx, permission.permNativeName,permission.description+"\n\nДавайте одобрим это разрешение?", {
                    ctx.requestPermissions(arrayOf(permission.permName), permission.permCode)
                }, {
                    // Рекурсивно вызываем для следующего разрешения
                    this.permissionListNext++
                    this.requestAppPermission(ctx)
                })
            } else -> {
                // Запрашиваем разрешение в первый раз
                Log.d("***", "Request "+permission.permNativeName+"!")
                ctx.requestPermissions(arrayOf(permission.permName), permission.permCode)
            }
        }
    }

    fun checkPermission(ctx: Activity, perm: String): Boolean {
        return ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
    }

    // проверка выбора пользователя, вызывается только после ответа на запрос
    // так-же вызывается если приложение сделало запрос но менеджер его не показал, по причине того, что пользователь поставил галку не спрашивать и запретил
    fun onRequestPermissionsResult(
        ctx: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        val permissionIndex = this.getPermissionIndex(this.permissionList, requestCode)
        if (permissionIndex==-1) return
        val permission = this.permissionList[permissionIndex]

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // разрешение получено
            // Рекурсивно вызываем для следующего разрешения
            this.permissionListNext++
            this.requestAppPermission(ctx)
        } else {
            // permission was not granted
            if (shouldShowRequestPermissionRationale(ctx, permission.permName)) {
                // сразу после первого отказа
                // можно показать почему нам надо это разрешение и еще раз спросить
                showPermissionRequestExplanation(ctx, permission.permNativeName+"!",permission.description+"\n\nДавайте одобрим это разрешение?", {
                    requestPermissions(ctx, arrayOf(permission.permName), permission.permCode)
                }, {
                    // Рекурсивно вызываем для следующего разрешения в случае отказа
                    this.permissionListNext++
                    this.requestAppPermission(ctx)
                })
            } else {
                // пользователь нажал "Не спрашивать больше" и запретил разрешение
                showPermissionRequestExplanation(ctx, permission.permNativeName+"!",permission.description+"\n\nХотите разрешить это в настройках?", {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    val uri: Uri = Uri.fromParts("package", ctx.packageName, null)
                    intent.data = uri
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    ctx.startActivity(intent)
                }, {
                    // Рекурсивно вызываем для следующего разрешения в случае запрета и отказа
                    this.permissionListNext++
                    this.requestAppPermission(ctx)
                })

            }
        }
    }

}