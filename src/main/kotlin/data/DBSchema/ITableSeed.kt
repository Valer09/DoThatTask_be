package homeaq.dothattask.data.TableCreationAndSeed

import java.sql.Connection

public interface ITableSeed
{
    fun seed(connection: Connection)
}