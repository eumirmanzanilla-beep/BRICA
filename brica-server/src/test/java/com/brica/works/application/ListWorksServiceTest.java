package com.brica.works.application;

import java.util.List;

import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListWorksServiceTest {

    @Mock
    private WorkStore workStore;

    @InjectMocks
    private ListWorksService listWorksService;

    @Test
    void shouldListWorks() {
        Work firstWork = mock(Work.class);
        Work secondWork = mock(Work.class);

        List<Work> works = List.of(
            firstWork,
            secondWork
        );

        when(workStore.list())
            .thenReturn(works);

        List<Work> result =
                listWorksService.listWorks();

        assertEquals(works, result);

        verify(workStore).list();
    }
}
