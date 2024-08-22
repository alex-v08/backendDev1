package com.spaceplanner.booking.reservation.service;

import com.spaceplanner.booking.reservation.entity.ReservationEntity;
import com.spaceplanner.booking.reservation.entity.ReservationStatus;
import com.spaceplanner.booking.reservation.entity.dto.ReservationDto;
import com.spaceplanner.booking.reservation.repository.IResorvationRepository;
import com.spaceplanner.booking.space.entity.Space;
import com.spaceplanner.booking.space.repository.ISpaceRepository;
import com.spaceplanner.booking.user.entity.User;
import com.spaceplanner.booking.user.repository.IUserRepository;
import jakarta.transaction.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;


@Service

public class ReservationService {

    private final IResorvationRepository reservationRepository;

    private final ISpaceRepository spaceRepository;

    private final IUserRepository userRepository;

    public ReservationService(IResorvationRepository reservationRepository, ISpaceRepository spaceRepository, IUserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
    }
    @Transactional
    public ReservationEntity createReservation(ReservationDto reservationDto) {

        validateReservationData(reservationDto);
        Space space = spaceRepository.findSpaceByNameCreation (reservationDto.getSpaceName());

        if (!isSpaceAvailable(space,reservationDto.getStartDate())  ) {
            throw new RuntimeException("Space is not available for the requested time.");
        }
       /* User user = userRepository.findByEmail (reservationDto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));*/
        ReservationEntity reservationEntity = new ReservationEntity();
        reservationEntity.setUser(userRepository.findByEmail (reservationDto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found")));
        reservationEntity.setSpace(space);
        reservationEntity.setStartDate (reservationDto.getStartDate ());
        reservationEntity.setStatus(ReservationStatus.APPROVED);

        return reservationRepository.save(reservationEntity);
    }

    @Transactional
    public boolean isSpaceAvailable(Space space, LocalDate startTime) {

        return reservationRepository.findAllBySpaceAndStartDate(space, startTime).isEmpty();
    }

    private void validateReservationData(ReservationDto reservationDto) {

        LocalDate startTime = reservationDto.getStartDate ();


        if (startTime.isAfter(now())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }


    }

    public Optional<ReservationEntity> findById(Long id) {

        return reservationRepository.findById(id);
    }
    public List<ReservationEntity> findByUserId(Long userId) {

        return reservationRepository.findAllByUser_Id(userId);
    }
    public List<ReservationEntity> findBySpaceId(Long spaceId) {
        return reservationRepository.findAllBySpace_Id(spaceId);
    }

    @Scheduled (cron = "0 0 0 * * *")
    @Transactional
    public void deleteReservation() {
      spaceRepository.findAll ().forEach (space -> {
            reservationRepository.findAllBySpaceAndStartDate (space, LocalDate.now ()).forEach (reservationEntity -> {
                if (reservationEntity.getStartDate ().isBefore (LocalDate.now ())) {
                    reservationEntity.setStatus (ReservationStatus.CANCELLED);
                    reservationRepository.save (reservationEntity);

                    space.setAvailable (true);
                }
            });
        });

    }
}
