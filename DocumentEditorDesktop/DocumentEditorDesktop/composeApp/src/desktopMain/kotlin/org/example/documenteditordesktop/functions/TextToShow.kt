package com.example.documenteditor.functions
// просто штук, для отображения текста одного из параметров
fun textToShow(showLeaveValueAlertFlagSetting: Boolean, isSaveValue:Boolean): String{
    return if(showLeaveValueAlertFlagSetting){
        "Спрашивать"
    }else{
        if(isSaveValue) "Сохранять"
        else "Не сохранять"
    }
}