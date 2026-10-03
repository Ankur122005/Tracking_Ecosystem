package in.mak.location;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrentLocationRepository
        extends JpaRepository<CurrentLocation, Long> {
}