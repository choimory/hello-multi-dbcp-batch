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
import org.springframework.context.annotation.Primary;
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
//yml의 spring.jpa 설정들을 편하게 사용하기 위한 어노테이션으로, JpaProperties 클래스를 열어보면 @ConfigurationProperties(prefix = "spring.jpa")가 붙어있음
//해당클래스 내 모든 JpaProperties 매개변수에 spring.jpa 관련 설정값들이 JpaProperties 클래스에 매핑되어 건내짐
//별도의 설정을 사용하길 원할시, 메소드나 클래스에 @ConfigurationProperties(prefix = "원하는 설정명")
@EnableConfigurationProperties(JpaProperties.class)
//반드시 설정해주어야 하는 어노테이션
@EnableJpaRepositories(basePackages = MainDataSourceConfig.PACKAGE, entityManagerFactoryRef = MainDataSourceConfig.ENTITY_MANAGER, transactionManagerRef = MainDataSourceConfig.TRANSACTION_MANAGER)
public class MainDataSourceConfig {
    /*패키지 경로 및 Bean명*/
    public static final String PACKAGE = "com.choimory.hello.hellomultidbcpbatch.main"; // DataSource는 최소 패키지별로 분류되어 사용되어야 함
    public static final String ENTITY_MANAGER = "mainEntityManager";
    public static final String TRANSACTION_MANAGER = "mainTransactionManager";
    public static final String DATA_SOURCE = "mainDataSource";
    public static final String DATA_SOURCE_PROPERTIES = "mainDataSourceProperties";
    public static final String JPA_VENDOR_ADAPTER = "mainJpaVendorAdapter";

    @Primary //복수개의 DBCP 사용시, 최소 하나의 빈에는 Primary가 적용되어 있어야 함. 배치 메타테이블은 Primary가 적용된 스키마에만 존재하면 됨
    @Bean(DATA_SOURCE_PROPERTIES)
    @ConfigurationProperties(prefix = "spring.datasource.main") //어떤 설정을 가져올지 - 여기선 main datasource의 기본 설정들
    //prefix 하위에 작성된 DataSource의 기본 설정들을 땡겨옴
    public DataSourceProperties dataSourceProperties(){
        return new DataSourceProperties();
    }

    @Primary
    @Bean(DATA_SOURCE)
    @ConfigurationProperties(prefix = "spring.datasource.main.hikari") //어떤 설정을 가져올지 - 여기선 main datasource의 hikari dbcp 설정들
    //dataSourceProperties()에서 땡겨온 DataSoure 기본 설정에 prefix 하위에 작성한 Hikari 설정을 더해서 DataSource를 build함
    public DataSource dataSource(){
        return dataSourceProperties().initializeDataSourceBuilder()
                                        .type(HikariDataSource.class)
                                        .build();
    }

    @Primary
    @Bean(JPA_VENDOR_ADAPTER)
    //spring.jpa 관련 설정들
    //show sql과 generate ddl을 굳이 set 안하고 그냥 인스턴스만 넘겨도 자동으로 적용되던데 이유는 모르겠음
    //yml에 data source 별로 설정을 다르게 작성하는법을 모르겠음. JpaProperties의 prefix가 정해져있어서 set할때 하드코딩 하는것 아니면 일괄 적용됨
    public JpaVendorAdapter jpaVendorAdapter(JpaProperties jpaProperties){
        AbstractJpaVendorAdapter jpaAdapter = new HibernateJpaVendorAdapter();
        jpaAdapter.setShowSql(jpaProperties.isShowSql());
        jpaAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return jpaAdapter;
    }

    @Primary
    @Bean(ENTITY_MANAGER)
    //Entity Manager 생성.
    // JpaProperties.getProperties()로 spring.jpa.properties..이하 항목들을 잘 넘기고는 있는데 적용은 안되고 있음
    public LocalContainerEntityManagerFactoryBean entityManager(@Qualifier(JPA_VENDOR_ADAPTER) JpaVendorAdapter jpaVendorAdapter
                                                                , ObjectProvider<PersistenceUnitManager> persistenceUnitManagers
                                                                , JpaProperties jpaProperties
                                                                , @Qualifier(DATA_SOURCE) DataSource dataSource){
        return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), persistenceUnitManagers.getIfAvailable()).dataSource(dataSource)
                                                                                                                                            .packages(PACKAGE)
                                                                                                                                            .build();
    }

    @Primary
    @Bean(TRANSACTION_MANAGER)
    public PlatformTransactionManager transactionManager(@Qualifier(ENTITY_MANAGER) LocalContainerEntityManagerFactoryBean entityManager){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManager.getObject());
        return transactionManager;
    }
}
