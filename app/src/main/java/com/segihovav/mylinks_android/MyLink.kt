package com.segihovav.mylinks_android

import android.os.Parcel
import android.os.Parcelable

data class MyLink(var ID: Int, var Name: String?, var URL: String?, var TypeID: Int) : Parcelable {
     constructor(parcel: Parcel) : this(
          parcel.readInt(),
          parcel.readString(),
          parcel.readString(),
          parcel.readInt()) { }

     companion object CREATOR : Parcelable.Creator<MyLink> {
          override fun createFromParcel(parcel: Parcel): MyLink {
               return MyLink(parcel)
          }

          override fun newArray(size: Int): Array<MyLink?> {
               return arrayOfNulls(size)
          }
     }

     override fun describeContents(): Int {
          return hashCode()
     }

     //write object values to parcel for storage
     override fun writeToParcel(dest: Parcel, flags: Int) {
          //write all properties
          dest.writeInt(ID)
          dest.writeString(Name)
          dest.writeString(URL)
          dest.writeInt(TypeID)
     }
}