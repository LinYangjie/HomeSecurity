package com.example.homesecurity

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()

        login_button.setOnClickListener {
            //get info from editText
            val user: String = userName_editText.text.toString().trim()
            val password: String = password_editText.text.toString().trim()
            val phone = phone_editText.text.toString().trim()
            checkLoginInfo(user, password, phone)
        }
        sign_up_text_view.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

    }


    private fun initialize() {
        db = FirebaseFirestore.getInstance()
    }

    private fun checkLoginInfo(userName: String, password: String, phone: String) {

        val userReference = db.collection("User")
        userReference.get().addOnSuccessListener { documentSnapshot ->
            val userList = documentSnapshot.toObjects(User::class.java)

            for (i in 0 until userList.size) {
                // check userName

                if (userName == userList[i].username) {
                    //there is username in db, then check password
                    if (password == userList[i].password) {
                        // check phone number
                        if (phone == userList[i].phone) {
                            val intent = Intent(this, UserActivity::class.java)
                            startActivity(intent)
                            break
                        } else { //wrong phone number
                            Toast.makeText(this, "Wrong phoneNumber", Toast.LENGTH_SHORT).show()
                            phone_editText.text.clear()
                            break

                        }

                    } else { //wrong password
                        Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                        password_editText.text.clear()
                        phone_editText.text.clear()
                        break
                    }

                }


                if (i == userList.size - 1) {
                    Toast.makeText(this, "Failed to Sign in", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to Sign in", Toast.LENGTH_SHORT).show()
        }
    }


}