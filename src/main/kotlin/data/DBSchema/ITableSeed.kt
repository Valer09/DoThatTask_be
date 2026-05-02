package homeaq.dothattask.data.TableCreationAndSeed

import java.sql.Connection

interface ITableSeed
{
    fun seed(connection: Connection)
}