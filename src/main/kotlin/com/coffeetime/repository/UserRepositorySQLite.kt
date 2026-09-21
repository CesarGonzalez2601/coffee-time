package com.coffeetime.repository

import com.coffeetime.database.DatabaseManager
import com.coffeetime.model.Administrator
import com.coffeetime.model.Cashier
import com.coffeetime.model.Credentials
import com.coffeetime.model.Role
import com.coffeetime.model.User
import java.sql.ResultSet

class UserRepositorySQLite : UserRepository {

    override fun create(
        name: String,
        pinHash: String,
        pinSalt: String,
        role: Role
    ): User {

        val sql = """
            INSERT INTO users (
                name,
                pin_hash,
                pin_salt,
                role
            )
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setString(1, name)
                statement.setString(2, pinHash)
                statement.setString(3, pinSalt)
                statement.setString(4, role.name)

                statement.executeUpdate()
            }

            connection.createStatement().use { statement ->

                statement.executeQuery(
                    "SELECT last_insert_rowid() AS id"
                ).use { result ->

                    if (result.next()) {

                        val id = result.getInt("id")

                        return createUser(
                            id = id,
                            name = name,
                            role = role,
                            active = true
                        )
                    }
                }
            }
        }

        throw IllegalStateException(
            "Could not create user."
        )
    }

    override fun findById(id: Int): User? {

        val sql = """
            SELECT
                id,
                name,
                role,
                active
            FROM users
            WHERE id = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setInt(1, id)

                statement.executeQuery().use { result ->

                    if (result.next()) {
                        return mapUser(result)
                    }
                }
            }
        }

        return null
    }

    override fun getCredentials(
        id: Int
    ): Credentials? {

        val sql = """
            SELECT
                pin_hash,
                pin_salt
            FROM users
            WHERE id = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setInt(1, id)

                statement.executeQuery().use { result ->

                    if (result.next()) {

                        return Credentials(
                            pinHash = result.getString(
                                "pin_hash"
                            ),
                            pinSalt = result.getString(
                                "pin_salt"
                            )
                        )
                    }
                }
            }
        }

        return null
    }

    override fun findAll(): List<User> {

        val users = mutableListOf<User>()

        val sql = """
            SELECT
                id,
                name,
                role,
                active
            FROM users
            ORDER BY id
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.executeQuery().use { result ->

                    while (result.next()) {

                        users.add(
                            mapUser(result)
                        )
                    }
                }
            }
        }

        return users
    }

    override fun count(): Int {

        val sql = """
            SELECT COUNT(*) AS total
            FROM users
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.executeQuery().use { result ->

                    if (result.next()) {
                        return result.getInt("total")
                    }
                }
            }
        }

        return 0
    }

    override fun updateStatus(
        id: Int,
        active: Boolean
    ): Boolean {

        val sql = """
            UPDATE users
            SET active = ?
            WHERE id = ?
        """.trimIndent()

        DatabaseManager.getConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setInt(
                    1,
                    if (active) 1 else 0
                )

                statement.setInt(2, id)

                return statement.executeUpdate() > 0
            }
        }
    }

    private fun mapUser(
        result: ResultSet
    ): User {

        val id = result.getInt("id")

        val name =
            result.getString("name")

        val role =
            Role.valueOf(
                result.getString("role")
            )

        val active =
            result.getInt("active") == 1

        return createUser(
            id = id,
            name = name,
            role = role,
            active = active
        )
    }

    private fun createUser(
        id: Int,
        name: String,
        role: Role,
        active: Boolean
    ): User {

        return when (role) {

            Role.ADMINISTRATOR ->
                Administrator(
                    id = id,
                    name = name,
                    active = active
                )

            Role.CASHIER ->
                Cashier(
                    id = id,
                    name = name,
                    active = active
                )
        }
    }
}