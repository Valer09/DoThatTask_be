package homeaq.dothattask

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
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.InviteRepository
import homeaq.dothattask.data.repository.RefreshTokenRepository
import homeaq.dothattask.data.repository.TaskRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.data.service.AuthService
import homeaq.dothattask.data.service.TaskService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.sql.Connection
import java.sql.DriverManager

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

    //Bean
    single<Connection>(qualifier = named("connection")) {
        val app = get<Application>()
        val useEmbedded = get<Boolean>( named("embedded"))

        if (useEmbedded) {
            app.log.info("Using embedded H2 database")
            DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")

        }
        else {
            val url = app.environment.config.property("ktor.postgres.url").getString()
            val user = app.environment.config.property("ktor.postgres.user").getString()
            val password = app.environment.config.property("ktor.postgres.password").getString()
            app.log.info("Connecting to postgres database at $url")
            DriverManager.getConnection(url, user, password)
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

    single<UserRepository> { UserRepository(
        get(named("connection")),
        get(named("user_table_factory")),
        get(named("user_table_seed"))) }

    single<GroupRepository> { GroupRepository(
        get(named("connection")),
        get(named("groups_table_factory")),
        get(named("groups_table_seed"))) }

    single<UserGroupRepository> { UserGroupRepository(
        get(named("connection")),
        get(named("user_groups_table_factory")),
        get(named("user_groups_table_seed"))) }

    single<TaskRepository> { TaskRepository(
            get(named("connection")),
        get(named("task_table_factory")),
        get(named("task_table_seed"))) }

    single<InviteRepository> { InviteRepository(
        get(named("connection")),
        get(named("invites_table_factory")),
        get(named("invites_table_seed"))) }

    single<RefreshTokenRepository> { RefreshTokenRepository(
        get(named("connection")),
        get(named("refresh_tokens_table_factory")),
        get(named("refresh_tokens_table_seed"))) }

    single<TaskService> { TaskService(get()) }

    single<JwtConfig> { JwtConfig(get<Application>().environment.config) }

    single<AuthService> {
        AuthService(
            jwt = get(),
            users = get(),
            refreshTokens = get(),
            userGroups = get(),
        )
    }

}
