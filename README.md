document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('restaurantSearch');
    if (!searchInput) return;

    const cards = [...document.querySelectorAll('.restaurant-card')];

    searchInput.addEventListener('input', (event) => {
        const query = event.target.value.trim().toLowerCase();

        cards.forEach((card) => {
            const text = card.dataset.name.toLowerCase();
            const visible = !query || text.includes(query);
            card.style.display = visible ? '' : 'none';
        });
    });
});
