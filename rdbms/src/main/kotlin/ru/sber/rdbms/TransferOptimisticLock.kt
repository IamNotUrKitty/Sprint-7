package ru.sber.rdbms

import java.sql.DriverManager
import java.sql.SQLException

class TransferOptimisticLock {
    val connection = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/db",
        "postgres", "postgres"
    )
    fun transfer(accountId1: Long, accountId2: Long, amount: Long) {
        connection.use { conn ->
            val autoCommit = conn.autoCommit
            try {
                conn.autoCommit = false
                val prepareStatement1 = conn.prepareStatement("select * from account1 where id = $accountId1")
                val account1 = prepareStatement1.use { statement ->
                    statement.executeQuery().use {
                        it.next()
                        Account(
                            it.getLong("id"),
                            it.getInt("amount"),
                            it.getInt("version")
                        )
                    }
                }

                val prepareStatement2 = conn.prepareStatement("select * from account1 where id = $accountId2")
                val account2 = prepareStatement2.use { statement ->
                    statement.executeQuery().use {
                        it.next()
                        Account(
                            it.getLong("id"),
                            it.getInt("amount"),
                            it.getInt("version")
                        )
                    }
                }



                if (account1.amount < account2.amount) throw CustomException("need more gold")

                val st1 = conn.prepareStatement("update account1 set amount = amount - $amount, version = version + 1 where id = ${account1.id}")
                st1.use { statement ->
                    val updatedRows = statement.executeUpdate()
                    if (updatedRows == 0) throw CustomException("Nothing to update")
                }
                val st2 = conn.prepareStatement("update account1 set amount = amount + $amount, version = version + 1 where id = ${account2.id}")
                st2.use { statement ->
                    val updatedRows = statement.executeUpdate()
                    if (updatedRows == 0) throw CustomException("Nothing to update")
                }
                conn.commit()
            } catch (e: CustomException) {
                e.printStackTrace()
                conn.rollback()
            } catch (e: SQLException) {
                e.printStackTrace()
                conn.rollback()
            } finally {
                conn.autoCommit = autoCommit
            }
        }
    }
}
