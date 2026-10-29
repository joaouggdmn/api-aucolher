package com.aucolher.api.event;

import com.aucolher.api.event.entity.Event;
import com.aucolher.api.event.entity.EventStatus;
import com.aucolher.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Exclusão da conta da ONG: o que acontece com os eventos dela, com o banco simulado (Mockito). */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository events;
    @Mock
    private EventAttendanceRepository attendances;
    @Mock
    private UserRepository users;

    @InjectMocks
    private EventService service;

    /** A consulta só traz os eventos ACTIVE de hoje em diante: os passados ficam como estão. */
    @Test
    void deletingTheNgoAccountCancelsUpcomingEventsWithAttendeesAndDeletesTheOthers() {
        Event withAttendees = event(4L, LocalDate.now().plusDays(3));
        Event empty = event(5L, LocalDate.now());
        when(events.findByNgoIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                1L, EventStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(empty, withAttendees));
        when(attendances.countByEventId(4L)).thenReturn(2L);
        when(attendances.countByEventId(5L)).thenReturn(0L);

        service.cancelUpcomingOf(1L);

        // Quem confirmou vê "Evento cancelado" em Minha conta
        assertThat(withAttendees.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(withAttendees.isRemovedByModeration()).isFalse();
        verify(events).delete(empty);
        verify(events, never()).delete(withAttendees);
    }

    private static Event event(Long id, LocalDate date) {
        Event event = new Event();
        event.setId(id);
        event.setNgoId(1L);
        event.setTitle("Feira de adoção");
        event.setDate(date);
        event.setStatus(EventStatus.ACTIVE);
        return event;
    }
}
