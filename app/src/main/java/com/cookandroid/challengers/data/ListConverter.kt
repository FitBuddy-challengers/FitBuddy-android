package com.cookandroid.challengers.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverter {
    //List<String>을 String으로 변환 (저장 시):
    //toStringList(list: List<String>?): String? 함수가 이 역할을 합니다.
    //Gson().toJson(it)을 사용하여 List<String> 객체를 JSON 문자열로 직렬화합니다.
    //예를 들어, listOf("사과", "바나나", "체리")는 ["사과","바나나","체리"]와 같은 JSON 문자열로 변환되어 Room 데이터베이스의 TEXT 컬럼에 저장될 수 있습니다.
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, listType)
        }
    }

    //String을 List<String>으로 변환 (읽어올 때):
    //fromStringList(value: String?): List<String>? 함수가 이 역할을 합니다.
    //데이터베이스에서 읽어온 JSON 문자열(value)을 Gson().fromJson(it, listType)을 사용하여 원래의 List<String> 객체로 역직렬화(파싱)합니다.
    //TypeToken은 Gson이 제네릭 타입(List<String>)을 올바르게 추론할 수 있도록 돕는 역할을 합니다.
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.let { Gson().toJson(it) }
    }
}