package com.example.homesecurity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*


class SignUpActivity : AppCompatActivity() {
    private var myUserList: List<User>? = null
    lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        db = FirebaseFirestore.getInstance()

        return_textView.setOnClickListener{ startActivity(Intent(this, MainActivity::class.java)) }

        submit_button.setOnClickListener {
            val user = User(userName_input_text.text.toString().trim(),
                            password_input_text.text.toString().trim(),
                            phone_input_text.text.toString().trim()
                            )
             if (checkValid(db,user)) {
                 db.collection("User").add(user
                 ).addOnSuccessListener {
                     startActivity(Intent(this, MainActivity::class.java))
                 }.addOnFailureListener {
                         Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
                 }
             }

            }

        userName_input_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {
                val userName = userName_input_text.text.toString().trim()
                val userReference = db.collection("User")
                userReference.get().addOnSuccessListener { documentSnapshot ->
                    val userList = documentSnapshot.toObjects(User::class.java)
                    // loop over userList to check if there is a repeat username
                    for (i in 0 until userList.size) {
                        if (userName == userList[i].username) {
                            Toast.makeText(
                                applicationContext,
                                "Username $userName have been used",
                                Toast.LENGTH_SHORT
                            ).show()
                            break
                        }
                    }
                }
            }
        })
    }

    private fun checkValid (db: FirebaseFirestore, user :User) : Boolean {
        val userReference = db.collection("User")
        userReference.get().addOnSuccessListener { documentSnapshot ->
            val userList = documentSnapshot.toObjects(User::class.java)
            myUserList = userList
        }
        if (myUserList != null) {
            for (checkUser in myUserList!!) {
                if (checkUser.username == user.username || checkUser.phone == user.phone) {
                    return false
                }
            }
        }
        return true
    }

}