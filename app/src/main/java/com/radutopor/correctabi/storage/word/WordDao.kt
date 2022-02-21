package com.radutopor.correctabi.storage.word

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {

    @Query("SELECT * FROM word WHERE path = :path")
    fun getWord(path: String): Word

    @Query("SELECT * FROM word WHERE path LIKE :parentPath || '_'")
    fun getChildWords(parentPath: String): List<Word>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addWord(word: Word)

    @Query("DELETE FROM word WHERE path LIKE :path || '%'")
    fun deleteWordTree(path: String)
}
