package homeaq.dothattask.data.TableCreationAndSeed

import java.sql.Connection

interface ITableFactory
{
    fun createTable(connection: Connection)
}