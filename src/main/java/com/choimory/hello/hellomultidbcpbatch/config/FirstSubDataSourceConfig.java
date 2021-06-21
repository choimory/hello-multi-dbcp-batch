package com.choimory.hello.hellomultidbcpbatch.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(basePackages = FirstSubDataSourceConfig.PACKAGE, entityManagerFactoryRef = FirstSubDataSourceConfig.ENTITY_MANAGER, transactionManagerRef = FirstSubDataSourceConfig.TRANSACTION_MANAGER)
public class FirstSubDataSourceConfig {
    public static final String PACKAGE = "com.choimory.hello.hellomultidbcpbatch.firstSub";
    public static final String ENTITY_MANAGER = "firstSubEntityManager";
    public static final String TRANSACTION_MANAGER = "firstSubTransactionManager";
    public static final String DATA_SOURCE = "firstSubDataSource";
    public static final String DATA_SOURCE_PROPERTIES = "firstSubDataSourceProperties";
    public static final String JPA_VENDOR_ADAPTER = "firstSubJpaVendorAdapter";

    @Bean(DATA_SOURCE_PROPERTIES)
    @ConfigurationProperties(prefix = "spring.datasource.first-sub")
    public DataSourceProperties dataSourceProperties(){
        return new DataSourceProperties();
    }

    @Bean(DATA_SOURCE)
    @ConfigurationProperties(prefix = "spring.datasource.first-sub.hikari")
    public DataSource dataSource(){
        return dataSourceProperties().initializeDataSourceBuilder()
                                        .type(HikariDataSource.class)
                                        .build();
    }

    @Bean(JPA_VENDOR_ADAPTER)
    public JpaVendorAdapter jpaVendorAdapter(JpaProperties jpaProperties){
        AbstractJpaVendorAdapter jpaAdapter = new HibernateJpaVendorAdapter();
        jpaAdapter.setShowSql(jpaProperties.isShowSql());
        jpaAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return jpaAdapter;
    }

    @Bean(ENTITY_MANAGER)
    public LocalContainerEntityManagerFactoryBean entityManager(@Qualifier(JPA_VENDOR_ADAPTER) JpaVendorAdapter jpaVendorAdapter
                                                                , ObjectProvider<PersistenceUnitManager> persistenceUnitManagers
                                                                , JpaProperties jpaProperties
                                                                , @Qualifier(DATA_SOURCE) DataSource dataSource){
        return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), persistenceUnitManagers.getIfAvailable()).dataSource(dataSource)
                                                                                                                                            .packages(PACKAGE)
                                                                                                                                            .build();
    }

    @Bean(TRANSACTION_MANAGER)
    public PlatformTransactionManager transactionManager(@Qualifier(ENTITY_MANAGER) LocalContainerEntityManagerFactoryBean entityManager){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManager.getObject());
        return transactionManager;
    }
}
