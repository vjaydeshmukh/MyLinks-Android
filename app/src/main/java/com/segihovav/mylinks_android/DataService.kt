package com.segihovav.mylinks_android

import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import java.util.ArrayList

class DataService {
     companion object {
          @JvmStatic lateinit var MyLinksURL: String
          @JvmStatic lateinit var sharedPreferences: SharedPreferences
          @JvmStatic var myLinksTypes: ArrayList<MyLinkType> = ArrayList()
          @JvmStatic var myLinksTypeNames: ArrayList<String> = ArrayList()
          @JvmStatic val getLinksDataEndpoint = "LinkData.php?task=fetchData"
          @JvmStatic val getTypesDataEndpoint = "LinkData.php?task=fetchTypes"
          @JvmStatic val deleteLinkDataEndpoint = "LinkData.php?task=deleteRow"
          @JvmStatic var lightMode = R.style.ThemeOverlay_MaterialComponents
          @JvmStatic var darkMode = R.style.ThemeOverlay_MaterialComponents_Dark
          @JvmStatic var URLS: MutableList<String> = mutableListOf()
          @JvmStatic var MyLinksTitle: String = "AbaLinks"

          @JvmStatic fun alert(builder: AlertDialog.Builder, message: String, closeApp: Boolean = false, confirmDialog: Boolean = false, finish: () -> Unit, OKCallback: (() -> Unit)?) {
               builder.setMessage(message).setCancelable(confirmDialog)

               builder.setPositiveButton("OK") { _, _ -> if (closeApp) finish() }

               if (confirmDialog) {
                    builder.setNegativeButton("Cancel") { _, _ -> if (closeApp) finish() }

                    if (OKCallback != null) {
                         builder.setPositiveButton("OK") { _, _ -> OKCallback() }
                    }
               }

               val alert = builder.create()

               alert.show()
          }

          @JvmStatic fun init() {
               if (URLS != null) {
                    val myLinksUrls = DataService.sharedPreferences.getStringSet("MyLinksURLs", mutableSetOf()) // get only ONCE

                    if (!myLinksUrls.isNullOrEmpty())
                         URLS.addAll(myLinksUrls)
               }
          }
     }
}