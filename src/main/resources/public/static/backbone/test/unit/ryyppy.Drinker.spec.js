describe('Drinker module', function () {
    it('Construction', function () {
        var drinker = new Drinker();
        expect(drinker).not.toBeUndefined();
        expect(drinker.get('name')).toBe('Anonymous');
        expect(drinker.get('drinks')).toBe(0);
    });

    it('Having drinks', function () {
        var drinker = new Drinker();
        expect(drinker.get('drinks')).toBe(0);
        drinker.drink();
        expect(drinker.get('drinks')).toBe(1);
    });
});