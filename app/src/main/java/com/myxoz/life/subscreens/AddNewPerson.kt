package com.myxoz.life.subscreens

//fun getAllContacts(context: Context): List<Pair<String, String>> {
//    val contacts = mutableListOf<Pair<String, String>>()
//    val resolver = context.contentResolver
//    val cursor = resolver.query(
//        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//        arrayOf(
//            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
//            ContactsContract.CommonDataKinds.Phone.NUMBER
//        ),
//        null, null, null
//    )
//
//    cursor?.use {
//        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
//        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
//        while (it.moveToNext()) {
//            val name = it.getString(nameIndex)
//            val number = it.getString(numberIndex)
//            contacts.add(name to number)
//        }
//    }
//
//    val knownNumbers = HashSet<String>()
//    return contacts.filter {
//        return@filter if(knownNumbers.contains(it.second.replace(" ", ""))) {
//            false
//        } else {
//            knownNumbers.add(it.second.replace(" ", ""))
//            true
//        }
//    }
//}