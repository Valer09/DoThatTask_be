package homeaq.dothattask.data.repository

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

class RefreshTokenRepository(
    private val connection: Connection,
    factory: ITableFactory,
    seeder: ITableSeed,
) {
    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }
}
