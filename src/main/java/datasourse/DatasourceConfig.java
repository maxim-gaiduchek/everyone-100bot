package datasourse;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import datasourse.repositories.BotChatRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
@EntityScan("/entities")
public class DatasourceConfig {

    private static final String JDBC_URL = "jdbc:postgresql://ec2-99-81-238-134.eu-west-1.compute.amazonaws.com:5432/d188ehfu7r5cmj";
    private static final String JDBC_USERNAME = "xrrcewayebeeur";
    private static final String JDBC_PASSWORD = "64e3f44a17ff8f2ef9651d71e08170eaec93d3449369bd81757e3064159c4b2e";
    private static final int JDBC_MAX_CONNECTION_POOL = 5;

    @Bean
    public DataSource dataSource() {
        HikariConfig dataSourceConfig = new HikariConfig();

        dataSourceConfig.setJdbcUrl(JDBC_URL);
        dataSourceConfig.setDriverClassName("org.postgresql.Driver");
        dataSourceConfig.setUsername(JDBC_USERNAME);
        dataSourceConfig.setPassword(JDBC_PASSWORD);
        dataSourceConfig.setMaximumPoolSize(JDBC_MAX_CONNECTION_POOL);

        return new HikariDataSource(dataSourceConfig);
    }

    @Bean(name = "service")
    public Service service(BotChatRepository repository) {
        return new JpaRepositoriesService(repository);
    }
}
