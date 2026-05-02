package homeaq.dothattask

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import homeaq.dothattask.data.DBSchema.GroupsTableFactoryH2
import homeaq.dothattask.data.DBSchema.GroupsTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.GroupsTableSeedH2
import homeaq.dothattask.data.DBSchema.GroupsTableSeedPostgres
import homeaq.dothattask.data.DBSchema.InvitesTableFactoryH2
import homeaq.dothattask.data.DBSchema.InvitesTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.InvitesTableSeedH2
import homeaq.dothattask.data.DBSchema.InvitesTableSeedPostgres
import homeaq.dothattask.data.DBSchema.RefreshTokensTableFactoryH2
import homeaq.dothattask.data.DBSchema.RefreshTokensTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.RefreshTokensTableSeedH2
import homeaq.dothattask.data.DBSchema.RefreshTokensTableSeedPostgres
import homeaq.dothattask.data.DBSchema.TaskTableFactoryH2
import homeaq.dothattask.data.DBSchema.TaskTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.TaskTableSeedH2
import homeaq.dothattask.data.DBSchema.TaskTableSeedPostgres
import homeaq.dothattask.data.DBSchema.UserGroupsTableFactoryH2
import homeaq.dothattask.data.DBSchema.UserGroupsTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.UserGroupsTableSeedH2
import homeaq.dothattask.data.DBSchema.UserGroupsTableSeedPostgres
import homeaq.dothattask.data.DBSchema.UserTableFactoryH2
import homeaq.dothattask.data.DBSchema.UserTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.UserTableSeedH2
import homeaq.dothattask.data.DBSchema.UserTableSeedPostgres
import homeaq.dothattask.Model.auth.JwtConfig
import homeaq.dothattask.Model.notifications.FirebaseConfig
import homeaq.dothattask.data.DBSchema.CategoriesTableFactoryH2
import homeaq.dothattask.data.DBSchema.CategoriesTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.CategoriesTableSeedH2
import homeaq.dothattask.data.DBSchema.CategoriesTableSeedPostgres
import homeaq.dothattask.data.DBSchema.FcmTokensTableFactoryH2
import homeaq.dothattask.data.DBSchema.FcmTokensTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.FcmTokensTableSeedH2
import homeaq.dothattask.data.DBSchema.FcmTokensTableSeedPostgres
import homeaq.dothattask.data.DBSchema.GroupCategoriesTableFactoryH2
import homeaq.dothattask.data.DBSchema.GroupCategoriesTableFactoryPostgres
import homeaq.dothattask.data.DBSchema.GroupCategoriesTableSeedH2
import homeaq.dothattask.data.DBSchema.GroupCategoriesTableSeedPostgres
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.repository.CategoryRepository
import homeaq.dothattask.data.repository.FcmTokenRepository
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.InviteRepository
import homeaq.dothattask.data.repository.RefreshTokenRepository
import homeaq.dothattask.data.repository.TaskRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.data.service.AuthService
import homeaq.dothattask.data.service.CategoryService
import homeaq.dothattask.data.service.GroupService
import homeaq.dothattask.data.service.InviteService
import homeaq.dothattask.data.service.NotificationService
import homeaq.dothattask.data.service.TaskService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import org.h2.tools.Server
import org.koin.core.qualifier.named
import org.koin.dsl.module
import javax.sql.DataSource

val appModule = module {

    single<Application> {
        getKoin().getProperty("application") ?: throw IllegalStateException("Application property is missing")
    }

    single<Boolean>(qualifier = named("embedded")) {
        get<Application>().environment.config
            .propertyOrNull("ktor.database.embedded")
            ?.getString()
            ?.toBoolean()
            ?: false
    }

    // HikariCP DataSource — replaces single raw Connection
    single<DataSource>(qualifier = named("dataSource")) {
        val app = get<Application>()
        val useEmbedded = get<Boolean>(named("embedded"))

        if (useEmbedded) {
            val h2Server = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
            h2Server.start()
            app.monitor.subscribe(ApplicationStopped) { h2Server.stop() }
            app.log.info("Using embedded H2 database (HikariCP pool)")
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                username = "root"
                password = ""
                driverClassName = "org.h2.Driver"
                maximumPoolSize = 10
            })
        } else {
            val url = app.environment.config.property("ktor.postgres.url").getString()
            val user = app.environment.config.property("ktor.postgres.user").getString()
            val password = app.environment.config.property("ktor.postgres.password").getString()
            app.log.info("Connecting to postgres at $url (HikariCP pool)")
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = url
                username = user
                this.password = password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                minimumIdle = 2
                connectionTimeout = 30_000
                idleTimeout = 600_000
                maxLifetime = 1_800_000
            })
        }
    }

    single<ITableFactory>(qualifier = named("task_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) TaskTableFactoryH2() else TaskTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("user_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserTableFactoryH2() else UserTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("groups_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) GroupsTableFactoryH2() else GroupsTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("user_groups_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserGroupsTableFactoryH2() else UserGroupsTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("invites_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) InvitesTableFactoryH2() else InvitesTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("refresh_tokens_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) RefreshTokensTableFactoryH2() else RefreshTokensTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("fcm_tokens_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) FcmTokensTableFactoryH2() else FcmTokensTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("categories_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) CategoriesTableFactoryH2() else CategoriesTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("group_categories_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) GroupCategoriesTableFactoryH2() else GroupCategoriesTableFactoryPostgres()
    }

    single<ITableSeed>(qualifier = named("task_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) TaskTableSeedH2() else TaskTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("user_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserTableSeedH2() else UserTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("groups_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) GroupsTableSeedH2() else GroupsTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("user_groups_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserGroupsTableSeedH2() else UserGroupsTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("invites_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) InvitesTableSeedH2() else InvitesTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("refresh_tokens_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) RefreshTokensTableSeedH2() else RefreshTokensTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("fcm_tokens_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) FcmTokensTableSeedH2() else FcmTokensTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("categories_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) CategoriesTableSeedH2() else CategoriesTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("group_categories_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) GroupCategoriesTableSeedH2() else GroupCategoriesTableSeedPostgres()
    }

    single<UserRepository> { UserRepository(
        get(named("dataSource")),
        get(named("user_table_factory")),
        get(named("user_table_seed")),
        isEmbedded = get(named("embedded"))
    ) }

    single<GroupRepository> { GroupRepository(
        get(named("dataSource")),
        get(named("groups_table_factory")),
        get(named("groups_table_seed"))) }

    single<UserGroupRepository> { UserGroupRepository(
        get(named("dataSource")),
        get(named("user_groups_table_factory")),
        get(named("user_groups_table_seed"))) }

    single<TaskRepository> { TaskRepository(
        get(named("dataSource")),
        get(named("task_table_factory")),
        get(named("task_table_seed"))) }

    single<InviteRepository> { InviteRepository(
        get(named("dataSource")),
        get(named("invites_table_factory")),
        get(named("invites_table_seed"))) }

    single<RefreshTokenRepository> { RefreshTokenRepository(
        get(named("dataSource")),
        get(named("refresh_tokens_table_factory")),
        get(named("refresh_tokens_table_seed"))) }

    single<FcmTokenRepository> { FcmTokenRepository(
        get(named("dataSource")),
        get(named("fcm_tokens_table_factory")),
        get(named("fcm_tokens_table_seed"))) }

    single<CategoryRepository> { CategoryRepository(
        dataSource = get(named("dataSource")),
        categoriesFactory = get(named("categories_table_factory")),
        categoriesSeeder = get(named("categories_table_seed")),
        groupCategoriesFactory = get(named("group_categories_table_factory")),
        groupCategoriesSeeder = get(named("group_categories_table_seed")),
    ) }

    single<TaskService> { TaskService(taskRepository = get(), categoryRepository = get()) }
    single<CategoryService> { CategoryService(categories = get()) }

    single<FirebaseConfig> { FirebaseConfig(get()) }
    single<NotificationService> { NotificationService(firebase = get(), fcmTokens = get(), users = get(), tasks = get()) }

    single<JwtConfig> { JwtConfig(get<Application>().environment.config) }

    single<AuthService> {
        AuthService(
            jwt = get(),
            userRepository = get(),
            refreshTokenRepository = get(),
            userGroupRepository = get(),
        )
    }

    single<GroupService> {
        GroupService(
            groups = get(),
            userGroups = get(),
            users = get(),
            categories = get(),
        )
    }

    single<InviteService> {
        InviteService(
            invites = get(),
            groups = get(),
            userGroups = get(),
            users = get(),
            notification = get()
        )
    }

}
