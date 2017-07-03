package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModelHelper;
import com.microsoft.azuretools.core.mvp.ui.base.TestSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ 
    AzureMvpModelHelper.class, 
    RedisPropertyViewPresenter.class,
    DefaultLoader.class,
})
public class RedisPropertyViewPresenterTest {

    @Mock
    private RedisPropertyMvpView redisPropertyMvpViewMock;

    @Mock
    private AzureMvpModelHelper azureMvpModelHelperMock;

    @Mock
    private RedisCacheProperty redisCachePropertyMock;

    private RedisPropertyViewPresenter<RedisPropertyMvpView> redisPropertyViewPresenter;

    private final static String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private final static String MOCK_ID = "test-id";

    private IDEHelper mockIDEHelper = new MockIDEHelper();
    private RedisCache redisCacheMock = new RedisCacheMock();

    @Before
    public void setUp() throws Exception {
        redisPropertyViewPresenter = new RedisPropertyViewPresenter<RedisPropertyMvpView>(TestSchedulerProvider.getInstance());
        redisPropertyViewPresenter.onAttachView(redisPropertyMvpViewMock);

        PowerMockito.mockStatic(AzureMvpModelHelper.class);
        when(AzureMvpModelHelper.getInstance()).thenReturn(azureMvpModelHelperMock);
        when(azureMvpModelHelperMock.getRedisCache(anyString(), anyString())).thenReturn(redisCacheMock);
        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getIdeHelper()).thenReturn(mockIDEHelper);
    }

    @Test
    public void testGetRedisProperty() throws Exception {
        redisPropertyViewPresenter.onGetRedisProperty(MOCK_SUBSCRIPTION, MOCK_ID);
        TestSchedulerProvider.getInstance().triggerActions();
        
        verify(redisPropertyMvpViewMock).showProperty(Mockito.any(RedisCacheProperty.class));
    }

    @After
    public void tearDown() {
        redisPropertyViewPresenter.onDetachView();
    }
}
