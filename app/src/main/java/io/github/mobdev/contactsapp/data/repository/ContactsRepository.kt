package io.github.mobdev.contactsapp.data.repository

import android.content.Context
import android.provider.ContactsContract
import io.github.mobdev.contactsapp.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.fetchAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<Contact>()

    val cursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        ),
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
    ) ?: return@withContext emptyList()

    cursor.use { c ->
        val idIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val nameIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

        while (c.moveToNext()) {
            val id = c.getLong(idIdx)
            val name = c.getString(nameIdx)?.takeIf { it.isNotBlank() }
            contacts.add(
                Contact(
                    id = id,
                    name = name,
                    phoneNumber = fetchFirstPhone(id),
                    email = fetchFirstEmail(id)
                )
            )
        }
    }

    contacts
}

private fun Context.fetchFirstPhone(contactId: Long): String? {
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    ) ?: return null

    return cursor.use {
        if (it.moveToFirst()) it.getString(0)?.takeIf { s -> s.isNotBlank() } else null
    }
}

private fun Context.fetchFirstEmail(contactId: Long): String? {
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    ) ?: return null

    return cursor.use {
        if (it.moveToFirst()) it.getString(0)?.takeIf { s -> s.isNotBlank() } else null
    }
}
