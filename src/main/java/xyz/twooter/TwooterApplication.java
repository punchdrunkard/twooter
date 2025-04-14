package xyz.twooter;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import xyz.twooter.configuration.JpaAuditingConfiguration;

@SpringBootApplication
@Import(JpaAuditingConfiguration.class)
public class TwooterApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwooterApplication.class, args);
	}

	@Profile("prod")
	@Bean
	public ApplicationRunner runner(DataSource dataSource) {
		return args -> {
			Connection connection = dataSource.getConnection();
		};
	}
}
