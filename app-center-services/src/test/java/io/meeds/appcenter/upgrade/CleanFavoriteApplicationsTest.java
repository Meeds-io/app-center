package io.meeds.appcenter.upgrade;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.persistence.impl.EntityManagerService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@SpringBootTest(classes = { CleanFavoriteApplications.class })
@ExtendWith(MockitoExtension.class)
public class CleanFavoriteApplicationsTest {

  @MockBean
  private EntityManagerService      entityManagerService;

  @MockBean
  private EntityManager             entityManager;

  @MockBean
  private Query                     query;

  @Autowired
  private CleanFavoriteApplications cleanFavoriteApplications;

  @BeforeEach
  void setUp() {
    cleanFavoriteApplications = new CleanFavoriteApplications(entityManagerService);
    when(entityManagerService.getEntityManager()).thenReturn(entityManager);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
  }

  @Test
  void processUpgradeCleanDuplicatedFavoriteApps() {
    List<Object[]> results = List.of(new Object[] { 1L, 100L, "user1" },
                                     new Object[] { 2L, 100L, "user1" },
                                     new Object[] { 3L, 101L, "test" },
                                     new Object[] { 4L, 101L, "test" });

    when(query.getResultList()).thenReturn(results);
    when(query.executeUpdate()).thenReturn(2);

    cleanFavoriteApplications.processUpgrade("v1", "v1");

    verify(entityManager, times(3)).createNativeQuery(anyString());
    verify(query, times(2)).executeUpdate();
    verify(entityManagerService, times(1)).getEntityManager();
  }

  @Test
  void processUpgradeNoDuplicatedFavoriteApps() {
    List<Object[]> results = List.of(new Object[] { 1L, 100L, "user1" }, new Object[] { 2L, 101L, "test" });

    when(query.getResultList()).thenReturn(results);

    cleanFavoriteApplications.processUpgrade("v1", "v1");

    verify(query, never()).executeUpdate();
  }
}
